package dev.faststats.screen;

import java.util.ArrayList;
import java.util.List;

final class SimpleDivision extends SimpleElement<Division> implements Division {
    public final List<Element<?>> elements = new ArrayList<>();
    public int width = 100, height = 100, gap = 0;
    public Orientation orientation = Orientation.VERTICAL;

    @Override
    public Division size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public Division gap(int pixels) {
        this.gap = pixels;
        return this;
    }

    @Override
    public Division add(Element<?> element) {
        this.elements.add(element);
        return this;
    }

    @Override
    public Division orientation(Orientation orientation) {
        this.orientation = orientation;
        return this;
    }
}
