package dev.faststats.screen;

import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

final class SimpleCheckbox extends SimpleElement<Checkbox> implements Checkbox {
    private @Nullable BiConsumer<Screen, Checkbox> stateChange = null;
    private boolean enabled = true, selected = true;
    private final String id;
    private final Text label;

    public SimpleCheckbox(final String id, final Text label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Text label() {
        return label;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public Checkbox enabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public boolean selected() {
        return selected;
    }

    @Override
    public Checkbox selected(final boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public Checkbox onStateChange(final BiConsumer<Screen, Checkbox> action) {
        if (stateChange == null) this.stateChange = action;
        else this.stateChange = this.stateChange.andThen(action);
        return this;
    }

    @Override
    public void onStateChange(final Screen screen) {
        if (stateChange != null) stateChange.accept(screen, this);
    }
}
