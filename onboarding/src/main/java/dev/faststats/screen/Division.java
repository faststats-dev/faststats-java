package dev.faststats.screen;

import java.util.List;

public sealed interface Division extends Element<Division> permits SimpleDivision {
    static Division div() {
        return new SimpleDivision();
    }

    List<Element<?>> elements();

    int gap();

    Division gap(int pixels);

    Division add(Element<?> element);

    Orientation orientation();

    Division orientation(Orientation orientation);

    enum Orientation {
        HORIZONTAL, VERTICAL
    }
}
