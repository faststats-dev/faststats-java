package dev.faststats.screen;

public sealed interface Element<T extends Element<T>> permits Button, Division, Checkbox, SimpleElement, TextBox {
    default T size(int height, int width) {
        return height(height).width(width);
    }

    int height();

    T height(int percentage);

    int width();

    T width(int percentage);
}
