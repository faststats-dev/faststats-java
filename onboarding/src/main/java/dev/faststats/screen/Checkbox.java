package dev.faststats.screen;

import java.util.function.BiConsumer;

public sealed interface Checkbox extends Element<Checkbox> permits SimpleCheckbox {
    static Checkbox create(final String id, final Text label) {
        return new SimpleCheckbox(id, label);
    }

    String id();

    Text label();

    boolean enabled();

    Checkbox enabled(boolean enabled);

    boolean selected();

    Checkbox selected(boolean selected);

    default boolean value() {
        return enabled() && selected();
    }

    Checkbox onStateChange(BiConsumer<Screen, Checkbox> action);

    void onStateChange(Screen screen);
}
