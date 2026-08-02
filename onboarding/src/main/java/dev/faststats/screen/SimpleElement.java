package dev.faststats.screen;

abstract non-sealed class SimpleElement<T extends Element<T>> implements Element<T> {
    protected int height = 100, width = 100;

    @Override
    public int height() {
        return height;
    }

    @Override
    public T height(final int percentage) {
        this.height = percentage;
        return self();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public T width(final int percentage) {
        this.width = percentage;
        return self();
    }
    
    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }
}
