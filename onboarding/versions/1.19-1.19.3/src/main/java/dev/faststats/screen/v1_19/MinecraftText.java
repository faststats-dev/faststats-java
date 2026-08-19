package dev.faststats.screen.v1_19;

import dev.faststats.screen.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;

public record MinecraftText(MutableComponent component) implements Text {
    @Override
    public Text append(final Text t) {
        component.append(((MinecraftText) t).component);
        return this;
    }

    @Override
    public Text append(final String t) {
        component.append(t);
        return this;
    }

    @Override
    public Text append(final String t, final URI u) {
        component.append(t);
        return this;
    }

    public Text color(final int c) {
        component.withStyle(s -> s.withColor(c));
        return this;
    }

    @Override
    public Text format(final Formatting f) {
        component.withStyle(ChatFormatting.valueOf(f.name()));
        return this;
    }
}
