package dev.faststats.screen;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SimpleScreen implements Screen {
    private final Text title;
    private final List<Element<?>> elements = new ArrayList<>();
    private final List<Element<?>> bottom = new ArrayList<>();
    private final List<Element<?>> footer = new ArrayList<>();
    private @Nullable Runnable onClose;

    public SimpleScreen(final Text title) {
        this.title = title;
    }

    @Override
    public Text title() {
        return title;
    }

    @Override
    public List<Element<?>> elements() {
        return List.copyOf(elements);
    }

    @Override
    public Screen add(final Element<?> element) {
        this.elements.add(element);
        return this;
    }

    @Override
    public List<Element<?>> bottom() {
        return List.copyOf(bottom);
    }

    @Override
    public Screen addBottom(final Element<?> element) {
        this.bottom.add(element);
        return this;
    }

    @Override
    public List<Element<?>> footer() {
        return List.copyOf(footer);
    }

    @Override
    public Screen addFooter(final Element<?> element) {
        this.footer.add(element);
        return this;
    }

    @Override
    public Optional<Checkbox> findSelect(final String id) {
        return findSelect(elements, id)
                .or(() -> findSelect(bottom, id))
                .or(() -> findSelect(footer, id));
    }

    @Override
    public Screen onClose(@Nullable final Runnable runnable) {
        this.onClose = runnable;
        return this;
    }

    @Override
    public Optional<Runnable> onClose() {
        return Optional.ofNullable(onClose);
    }

    private Optional<Checkbox> findSelect(final List<Element<?>> elements, final String id) {
        for (final var element : elements) {
            if (element instanceof final Checkbox checkbox && checkbox.id().equals(id)) {
                return Optional.of(checkbox);
            }
            if (element instanceof final Division division) {
                final var result = findSelect(division.elements(), id);
                if (result.isPresent()) return result;
            }
            if (element instanceof final Scrollable scrollable) {
                final var result = findSelect(scrollable.elements(), id);
                if (result.isPresent()) return result;
            }
        }
        return Optional.empty();
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
