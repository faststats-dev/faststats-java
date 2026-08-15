package dev.faststats.screen;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface Screen {
    static Screen screen(final Text title) {
        return new SimpleScreen(title);
    }

    Text title();

    List<Element<?>> elements();

    Screen add(Element<?> element);

    List<Element<?>> bottom();

    Screen addBottom(Element<?> element);

    List<Element<?>> footer();

    Screen addFooter(Element<?> element);

    Optional<Checkbox> findSelect(String id);

    Screen onClose(@Nullable Runnable runnable);

    Optional<Runnable> onClose();

    void close();

    void open();
}
