package dev.faststats.screen.v1_16;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.awt.Desktop;
import java.net.URI;

public final class MinecraftScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new MinecraftText(new TextComponent(""));
    }

    @Override
    public Text translatable(final String k) {
        return new MinecraftText(new TranslatableComponent(k));
    }

    @Override
    public void openScreen(final Screen s) {
        Minecraft.getInstance().setScreen(new ConsentScreen(s));
    }

    @Override
    public void closeScreen(final Screen s) {
        s.onClose().ifPresent(Runnable::run);
        if (Minecraft.getInstance().screen instanceof ConsentScreen) Minecraft.getInstance().setScreen(null);
    }

    private static final class ConsentScreen extends net.minecraft.client.gui.screens.Screen {
        private final Screen model;

        ConsentScreen(final Screen s) {
            super(((MinecraftText) s.title()).component());
            model = s;
        }

        @Override
        protected void init() {
            final int x = Math.max(8, width / 2 - 150);
            final int w = Math.min(300, width - 16);
            int y = Math.max(44, height / 2 - 70);
            toggle("submit_metrics", x, y, w);
            toggle("submit_additional_metrics", x, y += 24, w);
            toggle("submit_errors", x, y += 24, w);
            y += 28;
            addRenderableWidget(new Button(x, y, (w - 4) / 2, 20, new TextComponent("Unselect All"), b -> all(false)));
            addRenderableWidget(new Button(x + (w + 4) / 2, y, (w - 4) / 2, 20, new TextComponent("Select All"), b -> all(true)));
            addRenderableWidget(new Button(x, y + 24, w, 20, new TranslatableComponent("gui.done"), b -> close()));
            link("Info", "https://faststats.dev/info", x, height - 24, w / 2 - 1);
            link("Privacy", "https://faststats.dev/privacy", x + w / 2 + 1, height - 24, w / 2 - 1);
        }

        private void toggle(final String id, final int x, final int y, final int w) {
            final Checkbox c = model.findSelect(id).orElseThrow();
            final Button b = new Button(x, y, w, 20, label(c), ignored -> {
                c.selected(!c.value());
                c.onStateChange(model);
                refresh();
            });
            b.active = c.enabled();
            addRenderableWidget(b);
        }

        private Component label(final Checkbox c) {
            return new TextComponent(c.value() ? "[x] " : "[ ] ").append(((MinecraftText) c.label()).component());
        }

        private void all(final boolean v) {
            final Checkbox m = model.findSelect("submit_metrics").orElseThrow();
            m.selected(v);
            m.onStateChange(model);
            final Checkbox a = model.findSelect("submit_additional_metrics").orElseThrow();
            a.enabled(v).selected(v);
            a.onStateChange(model);
            final Checkbox e = model.findSelect("submit_errors").orElseThrow();
            e.selected(v);
            e.onStateChange(model);
            refresh();
        }

        private void link(final String l, final String u, final int x, final int y, final int w) {
            addRenderableWidget(new Button(x, y, w, 20, new TextComponent(l), b -> {
                if (Desktop.isDesktopSupported()) try {
                    Desktop.getDesktop().browse(URI.create(u));
                } catch (final Exception ignored) {
                }
            }));
        }

        private void refresh() {
            minecraft.setScreen(new ConsentScreen(model));
        }

        private void close() {
            model.onClose().ifPresent(Runnable::run);
            if (minecraft.screen == this) minecraft.setScreen(null);
        }

        @Override
        public void onClose() {
            close();
        }

        @Override
        public void render(final PoseStack p, final int mx, final int my, final float d) {
            renderBackground(p);
            drawCenteredString(p, font, title, width / 2, 16, 0xffffff);
            drawCenteredString(p, font, "Anonymous metrics and errors help mod developers.", width / 2, 30, 0xcccccc);
            super.render(p, mx, my, d);
        }
    }
}
