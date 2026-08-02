package dev.faststats.screen;

import java.util.ArrayList;
import java.util.List;

final class SimpleDivision extends SimpleElement<Division> implements Division {
    private final List<Element<?>> elements = new ArrayList<>();
    private int gap;
    private Orientation orientation = Orientation.VERTICAL;

    @Override
    public List<Element<?>> elements() {
        return List.copyOf(elements);
    }

    @Override
    public int gap() {
        return gap;
    }

    @Override
    public Division gap(final int pixels) {
        this.gap = pixels;
        return this;
    }

    @Override
    public Division add(final Element<?> element) {
        this.elements.add(element);
        return this;
    }

    @Override
    public Orientation orientation() {
        return orientation;
    }

    @Override
    public Division orientation(final Orientation orientation) {
        this.orientation = orientation;
        return this;
    }
}
