package dev.faststats.screen;

public sealed interface ScrollableTextBox extends Element<ScrollableTextBox> permits SimpleScrollableTextBox {
    static ScrollableTextBox of(final Text text) {
        return new SimpleScrollableTextBox(text);
    }

    Text text();

    ScrollableTextBox text(Text text);
}
