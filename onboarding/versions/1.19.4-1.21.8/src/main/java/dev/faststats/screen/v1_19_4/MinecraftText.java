package dev.faststats.screen.v1_19_4;

import dev.faststats.screen.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;

public record MinecraftText(MutableComponent component) implements Text {
    @Override
    public Text append(final Text text) {
        component.append(((MinecraftText) text).component);
        return this;
    }

    @Override
    public Text append(final String text) {
        component.append(text);
        return this;
    }

    @Override
    public Text append(final String text, final URI url) {
        component.append(text);
        return this;
    }

    public Text color(final int color) {
        component.withStyle(style -> style.withColor(color));
        return this;
    }

    @Override
    public Text format(final Formatting formatting) {
        component.withStyle(ChatFormatting.valueOf(formatting.name()));
        return this;
    }
}
