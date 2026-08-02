package dev.faststats.fabric.screen;

import dev.faststats.screen.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;

public final class FabricText implements Text {
    public final MutableComponent text = Component.empty();

    @Override
    public Text append(Text text) {
        this.text.append(((FabricText) text).text);
        return this;
    }

    @Override
    public Text append(String string) {
        this.text.append(string);
        return this;
    }

    @Override
    public Text append(String string, URI url) {
        this.text.append(Component.literal(string).withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(url))));
        return this;
    }

    @Override
    public Text color(int color) {
        this.text.withStyle(Style.EMPTY.withColor(color));
        return this;
    }

    @Override
    public Text format(Formatting formatting) {
        this.text.withStyle(toFormat(formatting));
        return this;
    }

    private ChatFormatting toFormat(Formatting formatting) {
        return switch (formatting) {
            case BLACK -> ChatFormatting.BLACK;
            case DARK_BLUE -> ChatFormatting.DARK_BLUE;
            case DARK_GREEN -> ChatFormatting.DARK_GREEN;
            case DARK_AQUA -> ChatFormatting.DARK_AQUA;
            case DARK_RED -> ChatFormatting.DARK_RED;
            case DARK_PURPLE -> ChatFormatting.DARK_PURPLE;
            case GOLD -> ChatFormatting.GOLD;
            case GRAY -> ChatFormatting.GRAY;
            case DARK_GRAY -> ChatFormatting.DARK_GRAY;
            case BLUE -> ChatFormatting.BLUE;
            case GREEN -> ChatFormatting.GREEN;
            case AQUA -> ChatFormatting.AQUA;
            case RED -> ChatFormatting.RED;
            case LIGHT_PURPLE -> ChatFormatting.LIGHT_PURPLE;
            case YELLOW -> ChatFormatting.YELLOW;
            case WHITE -> ChatFormatting.WHITE;
            case OBFUSCATED -> ChatFormatting.OBFUSCATED;
            case BOLD -> ChatFormatting.BOLD;
            case STRIKETHROUGH -> ChatFormatting.STRIKETHROUGH;
            case UNDERLINE -> ChatFormatting.UNDERLINE;
            case ITALIC -> ChatFormatting.ITALIC;
            case RESET -> ChatFormatting.RESET;
        };
    }
}
