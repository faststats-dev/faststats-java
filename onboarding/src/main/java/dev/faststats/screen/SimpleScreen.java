package dev.faststats.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SimpleScreen implements Screen {
    public final Text title;
    public final List<Element<?>> elements = new ArrayList<>();

    public SimpleScreen(Text title) {
        this.title = title;
    }

    @Override
    public Text title() {
        return title;
    }

    @Override
    public Screen add(Element<?> element) {
        this.elements.add(element);
        return this;
    }

    @Override
    public Optional<Checkbox> findSelect(String id) {
        return elements.stream()
                .filter(Checkbox.class::isInstance)
                .map(Checkbox.class::cast)
                .filter(select -> select.id().equals(id))
                .findAny();
    }

    @Override
    public void close() {
        ScreenManager.instance().closeScreen(this);
    }

    @Override
    public void open() {
        ScreenManager.instance().openScreen(this);
    }
}
