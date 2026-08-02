package dev.faststats.screen;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

final class SimpleButton extends SimpleElement<Button> implements Button {
    private @Nullable Consumer<Screen> clickAction = null;
    private final Text label;

    public SimpleButton(Text label) {
        this.label = label;
    }

    @Override
    public Button onClick(Consumer<Screen> action) {
        this.clickAction = action;
        return this;
    }

    @Override
    public Text label() {
        return label;
    }

    @Override
    public void onClick(Screen screen) {
        if (clickAction != null) clickAction.accept(screen);
    }
}
