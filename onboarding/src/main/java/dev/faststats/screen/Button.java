package dev.faststats.screen;

import java.util.function.BiConsumer;

public sealed interface Button extends Element<Button> permits SimpleButton {
    static Button button(final Text label) {
        return new SimpleButton(label);
    }

    boolean enabled();

    Button enabled(boolean enabled);

    Button onClick(BiConsumer<Screen, Button> action);

    Text label();

    void onClick(Screen screen);
}
