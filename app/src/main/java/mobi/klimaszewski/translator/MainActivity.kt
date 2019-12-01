package mobi.klimaszewski.translator

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.databinding.DataBindingUtil
import mobi.klimaszewski.translation.TranslatedContext
import mobi.klimaszewski.translation.TranslationInflaterFactory
import mobi.klimaszewski.translation.Translator
import mobi.klimaszewski.translator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val animator: ObjectAnimator by lazy {
        ObjectAnimator.ofPropertyValuesHolder(
            binding.state,
            PropertyValuesHolder.ofFloat("alpha", 0.3f, 1f)
        ).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            doOnEnd { binding.state.alpha = 1f }
        }
    }
    private val listener = object : Translator.OnStateChangedListener {
        override fun onStateChanged(state: Translator.State) {
            refreshSwitch(state)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(TranslatedContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        TranslationInflaterFactory.setup(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                toggle.isChecked = Translator.isEnabled(this@MainActivity)
                toggle.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (Translator.state == Translator.State.TranslationModelDownloaded) {
                        Translator.setEnabled(this@MainActivity, isChecked)
                    }
                }
            }
        Translator.addListener(listener)
        refreshSwitch(Translator.state)
    }

    private fun refreshSwitch(state: Translator.State) {
        binding.apply {
            when (state) {
                Translator.State.Initialising -> {
                    toggle.isClickable = false
                    toggle.alpha = 0.5f
                    binding.state.text = getString(R.string.state_init)
                }
                Translator.State.TranslationNotRequired -> {
                    binding.state.text = getString(R.string.state_not_required)
                    animator.end()
                }
                Translator.State.LocaleNotSupported -> {
                    binding.state.text = getString(R.string.state_not_supported)
                    animator.end()
                }
                Translator.State.TranslationModelDownloading -> {
                    binding.state.text = getString(R.string.state_downloading)
                    animator.start()
                }
                Translator.State.TranslationModelDownloaded -> {
                    toggle.isClickable = true
                    toggle.alpha = 1f
                    binding.state.text = getString(R.string.state_downloaded)
                    animator.end()
                }
                is Translator.State.TranslationModelDownloadError -> {
                    animator.end()
                    binding.state.text = getString(R.string.state_error)
                }
            }
        }
    }
}

