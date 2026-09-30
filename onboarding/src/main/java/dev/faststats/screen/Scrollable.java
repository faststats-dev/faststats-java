package dev.faststats.screen;

import java.util.List;

public sealed interface Scrollable extends Element<Scrollable> permits SimpleScrollable {
    static Scrollable create() {
        return new SimpleScrollable();
    }

    List<Element<?>> elements();

    int gap();

    Scrollable gap(int pixels);

    Scrollable add(Element<?> element);
}
