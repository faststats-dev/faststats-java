package dev.faststats.screen;

import java.util.ArrayList;
import java.util.List;

final class SimpleScrollable extends SimpleElement<Scrollable> implements Scrollable {
    private final List<Element<?>> elements = new ArrayList<>();
    private int gap = 8;

    @Override
    public List<Element<?>> elements() {
        return List.copyOf(elements);
    }

    @Override
    public int gap() {
        return gap;
    }

    @Override
    public Scrollable gap(final int pixels) {
        this.gap = pixels;
        return this;
    }

    @Override
    public Scrollable add(final Element<?> element) {
        elements.add(element);
        return this;
    }
}
