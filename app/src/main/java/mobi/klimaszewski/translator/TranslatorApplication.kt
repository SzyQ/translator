package mobi.klimaszewski.translator

import android.app.Application
import android.view.View
import com.google.firebase.FirebaseApp
import mobi.klimaszewski.translation.Translator.init
import mobi.klimaszewski.translation.parser.ParserFactory
import mobi.klimaszewski.translation.parser.ViewTranslationParser
import java.util.*

class TranslatorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        init(
            app = this,
            defaultAppLocale = Locale.ENGLISH,
            supportedLocales = listOf(Locale("pl")),
            factory = object : ParserFactory {
                override fun get(view: View): ViewTranslationParser? {
                    return null
                }
            },
            clazz = R.string::class.java
        )
    }
}