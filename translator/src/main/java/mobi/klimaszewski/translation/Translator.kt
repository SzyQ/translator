package mobi.klimaszewski.translation

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Message
import android.preference.Preference
import android.preference.PreferenceManager
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import mobi.klimaszewski.translation.parser.DefaultParserFactory
import mobi.klimaszewski.translation.parser.ParserFactory
import mobi.klimaszewski.translation.parser.ViewTranslationParser
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashMap
import kotlin.collections.set

object Translator {

    private const val KEY_TRANSLATOR = "KEY_TRANSLATOR"
    private var translator: FirebaseTranslator? = null
    private val views = CopyOnWriteArraySet<WeakReference<View>>()
    private val runner = Runner(this)
    private val processedViews = CopyOnWriteArraySet<String>()
    private val translations = ConcurrentHashMap<String, String>()
    private val viewParserFactories = mutableSetOf<ParserFactory>(DefaultParserFactory())
    private lateinit var app: Application
    private lateinit var defaultLocale: Locale
    private lateinit var supportedLocales: List<Locale>
    private var executor: ExecutorService? = null
    var animationEnabled = true
    lateinit var requiredLocale: Locale

    private var listeners = CopyOnWriteArrayList<WeakReference<OnStateChangedListener>>()
    var state: State = State.Initialising
        get() = field
        private set(value) {
            Timber.v("State: $value")
            field = value
            listeners.forEach { it.get()?.onStateChanged(value) }
        }

    private class Runner(private val translator: Translator) : Handler() {
        override fun handleMessage(msg: Message?) {
            invalidate()
        }
    }

    fun isEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_TRANSLATOR, false)
    }

    fun setEnabled(context: Context, isEnabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_TRANSLATOR, isEnabled)
            .apply()
        run(true)
    }

    /**
     * Reference to listener is weak so you need to keep reference to it
     */
    fun addListener(listener: OnStateChangedListener) {
        listeners.add(WeakReference(listener))
    }

    @JvmOverloads
    fun init(
        app: Application,
        defaultAppLocale: Locale,
        supportedLocales: List<Locale>,
        factory: ParserFactory? = null,
        clazz: Class<*>? = null
    ) {
        Translator.app = app
        factory?.let { viewParserFactories.add(factory) }
        defaultLocale = defaultAppLocale
        Translator.supportedLocales = supportedLocales
        requiredLocale = Locale.getDefault()
        if (!isTranslationRequired()) {
            state = State.TranslationNotRequired
            return
        }
        val sourceLanguage =
            FirebaseTranslateLanguage.languageForLanguageCode(defaultAppLocale.language)
        val desiredLanguage =
            FirebaseTranslateLanguage.languageForLanguageCode(requiredLocale.language)
        if (sourceLanguage == null || desiredLanguage == null) {
            state = State.LocaleNotSupported
            return
        }
        state = State.TranslationModelDownloading
        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(desiredLanguage)
            .build()
        translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
        translator?.downloadModelIfNeeded()
            ?.addOnSuccessListener {
                Timber.d("Translations loaded")
                state = State.TranslationModelDownloaded
                run()
                clazz?.let { prefetch(it) }
            }
            ?.addOnFailureListener {
                Timber.e(it, "Translations failed to load")
                state = State.TranslationModelDownloadError(it)
                cleanup()
            }
    }

    private fun prefetch(clazz: Class<*>) {
        Timber.d("Prefetching translations")
        executor?.shutdown()
        executor = Executors.newSingleThreadExecutor()
        executor?.submit {
            val fields = clazz.declaredFields
            for (i in fields.indices) {
                val resId = app.resources.getIdentifier(fields[i].name, "string", app.packageName)
                if (resId != 0) {
                    app.resources.getString(resId).apply {
                        Timber.v("Fetching ${fields[i].name} - $this")
                        fetchTranslation(this)
                    }
                }
            }
        }
    }

    fun isTranslationAvailable() =
        FirebaseTranslateLanguage.languageForLanguageCode(defaultLocale.language) != null &&
                FirebaseTranslateLanguage.languageForLanguageCode(requiredLocale.language) != null

    fun isTranslationRequired() = !supportedLocales.map { it.language }.contains(
        requiredLocale.language
    )

    @Synchronized
    fun add(view: View) {
        views.add(WeakReference(view))
    }

    fun run(forceRefresh: Boolean = false) {
        if ((isTranslationRequired() && isEnabled(
                app
            )) || forceRefresh
        ) {
            runner.removeMessages(0)
            runner.sendEmptyMessageDelayed(0, 16)
        }
    }

    fun cleanup() {
        val modelManager = FirebaseModelManager.getInstance()
        modelManager.getDownloadedModels(FirebaseTranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                models.forEach { model ->
                    modelManager.deleteDownloadedModel(model)
                        .addOnSuccessListener {
                            Timber.d("Deleted translation model ${model.languageCode}")
                        }
                        .addOnFailureListener {
                            Timber.w(it, "Failed to delete translation model ${model.languageCode}")
                        }
                }
            }
            .addOnFailureListener {
                Timber.w(it, "Failed to cleanup translation models")
            }
    }

    private fun invalidate() {
        if (isEnabled(app)) {
            enable()
        } else {
            disable()
        }
    }

    @Synchronized
    private fun disable() {
        Timber.d("Disabling translation")
        if (translations.isEmpty()) {
            return
        }
        val reversed = translations.reversed()

        views.forEach {
            it.get()?.apply {
                findParser(this)?.let { parser ->
                    parser.text = parser.text.map { reversed[it] }
                }
            }
        }
        processedViews.clear()
    }

    fun findParser(view: View): ViewTranslationParser? {
        viewParserFactories.forEach {
            val parser = it.get(view)
            if (parser != null) {
                return parser
            }
        }
        return null
    }

    private fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
        entries.forEach { newMap.put(it.value, it.key) }
    }

    @Synchronized
    private fun enable() {
        Timber.d("Enabling translation")
        views.forEach { reference ->
            reference.get()?.let { view ->
                val isViewAdded = processedViews.add(view.toStringHash())
                if (isViewAdded) {
                    findParser(view)?.let { parser ->
                        if (parser.text.filterNotNull().isNotEmpty()) {
                            if (animationEnabled) {
                                view.animate()
                                    .alphaBy(-0f)
                                    .setDuration(160)
                                    .setInterpolator(AccelerateInterpolator())
                                    .withEndAction {
                                        fetch(parser)
                                    }
                                    .start()
                            } else {
                                fetch(parser)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetch(view: ViewTranslationParser) {
        val toTranslate =
            view.text.mapIndexed { index, text -> Pair(index, text) }.filter { it.second != null }
        val iterator = toTranslate.listIterator()
        val translatedTexts = view.text.toMutableList()
        translator?.apply {
            findTranslations(iterator, translatedTexts, view)
        }
    }

    private fun FirebaseTranslator.findTranslations(
        iterator: ListIterator<Pair<Int, String?>>,
        translatedTexts: MutableList<String?>,
        view: ViewTranslationParser
    ) {
        if (iterator.hasNext() && view.view != null) {
            val pair = iterator.next()
            val cachedTranslation =
                mobi.klimaszewski.translation.Translator.translations[pair.second]
            if (cachedTranslation != null) {
                translatedTexts[pair.first] = cachedTranslation
                findTranslations(iterator, translatedTexts, view)
            } else {
                translate(pair.second!!)
                    .addOnSuccessListener {
                        Timber.v("Got translation: ${pair.second}=$it")
                        mobi.klimaszewski.translation.Translator.translations[pair.second!!] = it
                        if (!mobi.klimaszewski.translation.isTranslationSameAsOriginal(
                                pair.second,
                                it
                            )
                        ) {
                            translatedTexts[pair.first] = it
                        }
                    }
                    .addOnFailureListener {
                        Timber.w("Failed to translate ${pair.second}")
                    }
                    .addOnCompleteListener {
                        Timber.v("Looking for next translation")
                        findTranslations(iterator, translatedTexts, view)
                    }
            }
        } else {
            view.text = translatedTexts
            if (mobi.klimaszewski.translation.Translator.animationEnabled) {
                view.view?.apply {
                    animate()
                        .alphaBy(1f)
                        .setDuration(320)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        }
    }

    fun translate(string: String): String {
        return if (isEnabled(app)) translations[string]
            ?: string else string
    }

    fun translateTask(text: String): Task<String>? {
        return translator?.translate(text)
    }

    private fun fetchTranslation(string: String) {
        translator?.apply {
            try {
                val translation = Tasks.await(translate(string))
                if (!isTranslationSameAsOriginal(string, translation)) {
                    translations[string] = translation
                }
            } catch (e: ExecutionException) {
                Timber.e(e, "Couldn't fetch translation")
            } catch (e: InterruptedException) {
                Timber.e(e, "Couldn't fetch translation")
            } catch (e: Throwable) {
                Timber.e(e, "Couldn't fetch translation")
            }
        }
    }

    /**
     * Forces {@link Locale} on the given context
     *
     * @param context can be Application Context or Activity, depending on what scope changes should take
     * @param locale  {@link Locale} to force
     */
    @JvmStatic
    fun forceLocale(context: Context, locale: Locale?): Context {
        if (locale == null) {
            return context;
        }
        Timber.d("Forcing " + locale.getDisplayLanguage());
        val res = context.resources;
        Locale.setDefault(locale);
        val config = Configuration(res.configuration);
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.displayMetrics);
        }

        return context;
    }

    @JvmStatic
    fun translate(preference: Preference) {
        val title = translate(preference.title.toString())
        val summary = translate(preference.summary.toString())
        preference.title = title
        preference.summary = summary
    }

    sealed class State {
        object Initialising : State()
        object TranslationNotRequired : State()
        object LocaleNotSupported : State()
        object TranslationModelDownloading : State()
        object TranslationModelDownloaded : State()
        data class TranslationModelDownloadError(val throwable: Throwable) : State()
    }

    interface OnStateChangedListener {
        fun onStateChanged(state: State)
    }
}

private fun View?.toStringHash() = this.hashCode().toString()

private fun isTranslationSameAsOriginal(text: CharSequence?, translation: String) =
    text == null || text.toString().cleanup().equals(translation.cleanup(), ignoreCase = true)

private fun String.cleanup(): String = trim()
    .replace("：", ":", true)
    .replace(Regex(" "), "")
    .replace(Regex("\\s"), "")