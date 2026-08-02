package dev.faststats.screen;

import java.util.function.Consumer;

public sealed interface Button extends Element<Button> permits SimpleButton {
    static Button button(Text label) {
        return new SimpleButton(label);
    }

    Button onClick(Consumer<Screen> action);
    
    Text label();
    
    void onClick(Screen screen);
}
