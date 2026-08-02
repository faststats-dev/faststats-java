package dev.faststats.screen;

public sealed interface Element<T extends Element<T>> permits Button, Checkbox, Division, Scrollable, ScrollableTextBox, SimpleElement, TextBox {
    default T size(final int width, final int height) {
        return width(width).height(height);
    }

    int height();

    T height(int percentage);

    int width();

    T width(int percentage);
}
