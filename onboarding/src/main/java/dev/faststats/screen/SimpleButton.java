package dev.faststats.screen;

import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

final class SimpleButton extends SimpleElement<Button> implements Button {
    private @Nullable BiConsumer<Screen, Button> clickAction = null;
    private boolean enabled = true;
    private final Text label;

    public SimpleButton(final Text label) {
        this.label = label;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public Button enabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public Button onClick(final BiConsumer<Screen, Button> action) {
        this.clickAction = action;
        return this;
    }

    @Override
    public Text label() {
        return label;
    }

    @Override
    public void onClick(final Screen screen) {
        if (enabled && clickAction != null) clickAction.accept(screen, this);
    }
}
