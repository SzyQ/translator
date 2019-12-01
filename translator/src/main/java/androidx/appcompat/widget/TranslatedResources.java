package androidx.appcompat.widget;

import android.content.res.Resources;
import mobi.klimaszewski.translation.Translator;
import org.jetbrains.annotations.NotNull;

public class TranslatedResources extends ResourcesWrapper {

    private Translator translator = Translator.INSTANCE;

    public TranslatedResources(final Resources resources) {
        super(resources);
    }

    @NotNull
    @Override
    public String getString(final int id) throws NotFoundException {
        return translator.translate(super.getString(id));
    }

    @NotNull
    @Override
    public String getString(final int id, final Object... formatArgs) throws NotFoundException {
        return translator.translate(super.getString(id, formatArgs));
    }

    @Override
    public CharSequence getText(final int id) throws NotFoundException {
        return translator.translate(super.getText(id).toString());
    }

    @Override
    public CharSequence getText(final int id, final CharSequence def) {
        return translator.translate(super.getText(id, def).toString());
    }
}
