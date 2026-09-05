package dev.faststats.screen;

import java.net.URI;

public interface Text {
    static Text text() {
        return ScreenManager.instance().newText();
    }

    static Text translatable(final String text) {
        return ScreenManager.instance().translatable(text);
    }

    static Text of(final String text) {
        return text().append(text);
    }

    static Text url(final String text, final URI uri) {
        return text().append(text, uri)
                .format(Formatting.AQUA)
                .format(Formatting.UNDERLINE);
    }

    static Text url(final URI uri) {
        return url(uri.toString(), uri);
    }

    Text append(Text text);

    Text append(String string);

    Text append(String text, URI url);

    default Text append(final URI url) {
        return append(url.toString(), url);
    }

    Text format(Formatting formatting);

    default TextBox box() {
        return TextBox.of(this);
    }

    enum Formatting {
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_AQUA,
        DARK_RED,
        DARK_PURPLE,
        GOLD,
        GRAY,
        DARK_GRAY,
        BLUE,
        GREEN,
        AQUA,
        RED,
        LIGHT_PURPLE,
        YELLOW,
        WHITE,
        OBFUSCATED,
        BOLD,
        STRIKETHROUGH,
        UNDERLINE,
        ITALIC,
        RESET
    }
}
