package dev.faststats.screen;

public sealed interface TextBox extends Element<TextBox> permits SimpleTextBox {
    static TextBox of(Text text) {
        return new SimpleTextBox(text);
    }
    
    Text text();
}
