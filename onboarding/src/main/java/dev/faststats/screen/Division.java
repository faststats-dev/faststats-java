package dev.faststats.screen;

public sealed interface Division extends Element<Division> permits SimpleDivision {
    static Division div() {
        return new SimpleDivision();
    }

    Division size(int width, int height);

    Division gap(int pixels);

    Division add(Element<?> element);

    Division orientation(Orientation orientation);

    enum Orientation {
        HORIZONTAL, VERTICAL
    }
}
