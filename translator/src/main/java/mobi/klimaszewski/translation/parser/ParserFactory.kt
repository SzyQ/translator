package mobi.klimaszewski.translation.parser

import android.text.TextUtils
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import java.lang.ref.WeakReference

class DefaultParserFactory : ParserFactory {

    override fun get(view: View): ViewTranslationParser? {
        return when (view) {
            is EditText -> EditTextParser(WeakReference(view))
            is TextView -> TextViewParser(WeakReference(view))
            is CheckBox -> CheckBoxParser(WeakReference(view))
            is TextInputLayout -> TextInputLayoutParser(WeakReference(view))
            is Toolbar -> ToolbarParser(WeakReference(view))
            else -> null
        }
    }
}

interface ParserFactory {
    fun get(view: View): ViewTranslationParser?
}

class TextViewParser(private val reference: WeakReference<TextView>) :
        ViewTranslationParser {

    override val view: View?
        get() = reference.get()

    override var text: List<String?>
        get() = listOf(validate(reference.get()?.text.toString()))
        set(value) {
            value.forEachIndexed { index, text ->
                if (text == null) {
                    return@forEachIndexed
                }
                when (index) {
                    0 -> reference.get()?.text = text
                }
            }
        }
}

class TextInputLayoutParser(private val reference: WeakReference<TextInputLayout>) :
        ViewTranslationParser {

    override val view: View?
        get() = reference.get()

    override var text: List<String?>
        get() = listOf(
                validate(reference.get()?.hint?.toString())
        )
        set(value) {
            value.forEachIndexed { index, text ->
                if (text == null) {
                    return@forEachIndexed
                }
                when (index) {
                    0 -> reference.get()?.hint = text
                }
            }
        }
}

class CheckBoxParser(private val reference: WeakReference<CheckBox>) :
        ViewTranslationParser {

    override val view: View?
        get() = reference.get()

    override var text: List<String?>
        get() = listOf(validate(reference.get()?.text.toString()))
        set(value) {
            value.forEachIndexed { index, text ->
                if (text == null) {
                    return@forEachIndexed
                }
                when (index) {
                    0 -> reference.get()?.text = text
                }
            }
        }
}

class EditTextParser(private val reference: WeakReference<EditText>) :
        ViewTranslationParser {

    override val view: View?
        get() = reference.get()

    override var text: List<String?>
        get() = listOf(
                validate(reference.get()?.text?.toString()),
                validate(reference.get()?.hint?.toString())
        )
        set(value) {
            value.forEachIndexed { index, text ->
                if (text == null) {
                    return@forEachIndexed
                }
                when (index) {
                    0 -> reference.get()?.setText(text)
                    1 -> reference.get()?.hint = text
                }
            }
        }
}

class ToolbarParser(private val reference: WeakReference<Toolbar>) :
        ViewTranslationParser {

    override val view: View?
        get() = reference.get()

    override var text: List<String?>
        get() = listOf(
                validate(reference.get()?.title?.toString()),
                validate(reference.get()?.subtitle?.toString())
        )
        set(value) {
            value.forEachIndexed { index, text ->
                if (text == null) {
                    return@forEachIndexed
                }
                when (index) {
                    0 -> reference.get()?.title = text
                    1 -> reference.get()?.subtitle = text
                }
            }
        }
}

interface ViewTranslationParser {
    val view: View?
    var text: List<String?>
}

private fun validate(text: String?) = if (text != null && text.isNotEmpty() && !TextUtils.isDigitsOnly(text)) text else null