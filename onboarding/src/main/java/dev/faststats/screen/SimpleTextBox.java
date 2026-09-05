package dev.faststats.screen;

final class SimpleTextBox extends SimpleElement<TextBox> implements TextBox {
    private final Text text;

    public SimpleTextBox(final Text text) {
        this.text = text;
    }

    @Override
    public Text text() {
        return text;
    }
}
