package mobi.klimaszewski.translation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.appcompat.widget.TranslatedResources

class TranslatedContext(context: Context) : ContextWrapper(context) {

    companion object {
        @JvmStatic
        fun wrap(context: Context): Context = TranslatedContext(context)
    }

    override fun getResources(): Resources {
        val resources = super.getResources()
        return if (resources is TranslatedResources) {
            resources
        } else {
            TranslatedResources(resources)
        }
    }
}