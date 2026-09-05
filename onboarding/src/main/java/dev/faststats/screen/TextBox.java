package dev.faststats.screen;

public sealed interface TextBox extends Element<TextBox> permits SimpleTextBox {
    static TextBox of(final Text text) {
        return new SimpleTextBox(text);
    }

    default ScrollableTextBox scrollable() {
        return ScrollableTextBox.of(text()).size(width(), height());
    }

    Text text();
}
