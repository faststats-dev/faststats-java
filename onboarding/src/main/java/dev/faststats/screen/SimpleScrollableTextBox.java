package dev.faststats.screen;

final class SimpleScrollableTextBox extends SimpleElement<ScrollableTextBox> implements ScrollableTextBox {
    private Text text;

    SimpleScrollableTextBox(final Text text) {
        this.text = text;
    }

    @Override
    public Text text() {
        return text;
    }

    @Override
    public ScrollableTextBox text(final Text text) {
        this.text = text;
        return this;
    }
}
