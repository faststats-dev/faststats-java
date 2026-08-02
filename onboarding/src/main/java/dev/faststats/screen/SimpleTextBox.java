package dev.faststats.screen;

final class SimpleTextBox extends SimpleElement<TextBox> implements TextBox {
    private final Text text;

    public SimpleTextBox(Text text) {
        this.text = text;
    }

    @Override
    public Text text() {
        return text;
    }
}
