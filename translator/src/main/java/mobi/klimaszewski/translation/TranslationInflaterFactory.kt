package mobi.klimaszewski.translation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.LayoutInflater.Factory2
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.LayoutInflaterCompat
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout

class TranslationInflaterFactory(
        private val originalFactory: Factory2
) : Factory2 {

    companion object {
        fun setup(activity: AppCompatActivity) {
            val layoutInflater = LayoutInflater.from(activity)
            if (layoutInflater.factory == null) {
                LayoutInflaterCompat.setFactory2(
                        layoutInflater,
                        TranslationInflaterFactory(
                            activity.delegate as Factory2
                        )
                )
            }
        }
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        var view = originalFactory.onCreateView(parent, name, context, attrs)
        if (view == null) {
            view = tryCreateView(name, context, attrs)
        }
        return view?.apply {
            Translator.add(this)
            Translator.run()
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        var view = originalFactory.onCreateView(name, context, attrs)
        if (view == null) {
            view = tryCreateView(name, context, attrs)
        }
        return view?.apply {
            Translator.add(this)
            Translator.run()
        }
    }

    //TODO This should be bound with custom view factories
    private fun tryCreateView(name: String, context: Context, attrs: AttributeSet?): View? {
        return when (name) {
            "com.google.android.material.chip.Chip" -> createChip(context, attrs)
            "com.google.android.material.textfield.TextInputLayout" -> createTextInputLayout(context, attrs)
            "androidx.appcompat.widget.Toolbar" -> createToolbar(context, attrs)
            else -> null
        }
    }

    private fun createToolbar(context: Context, attrs: AttributeSet?): Toolbar {
        return Toolbar(context, attrs)
    }

    private fun createTextInputLayout(context: Context, attrs: AttributeSet?): View? {
        return TextInputLayout(context, attrs)
    }

    private fun createChip(context: Context, attrs: AttributeSet?): Chip {
        return Chip(context, attrs)
    }

}