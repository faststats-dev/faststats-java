package dev.faststats.screen.v1_19;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class MinecraftScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new MinecraftText(Component.empty());
    }

    @Override
    public Text translatable(final String k) {
        return new MinecraftText(Component.translatable(k));
    }

    @Override
    public void openScreen(final Screen s) {
        Minecraft.getInstance().setScreen(new Wrapper(s));
    }

    @Override
    public void closeScreen(final Screen s) {
        s.onClose().ifPresent(Runnable::run);
        if (Minecraft.getInstance().screen instanceof Wrapper) Minecraft.getInstance().setScreen(null);
    }

    private static final class Wrapper extends net.minecraft.client.gui.screens.Screen {
        final Screen model;

        Wrapper(final Screen s) {
            super(((MinecraftText) s.title()).component());
            model = s;
        }

        @Override
        protected void init() {
            final int x = width / 2 - 150;
            final int y = Math.max(45, height / 2 - 65);
            toggle("submit_metrics", x, y);
            toggle("submit_additional_metrics", x, y + 24);
            toggle("submit_errors", x, y + 48);
            addRenderableWidget(Button.builder(Component.literal("Unselect All"), b -> all(false)).bounds(x, y + 76, 148, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Select All"), b -> all(true)).bounds(x + 152, y + 76, 148, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> close()).bounds(x, y + 100, 300, 20).build());
        }

        void toggle(final String id, final int x, final int y) {
            final Checkbox c = model.findSelect(id).orElseThrow();
            final Button b = Button.builder(Component.literal(c.value() ? "[x] " : "[ ] ").append(((MinecraftText) c.label()).component()), ignored -> {
                c.selected(!c.value());
                c.onStateChange(model);
                minecraft.setScreen(new Wrapper(model));
            }).bounds(x, y, 300, 20).build();
            b.active = c.enabled();
            addRenderableWidget(b);
        }

        void all(final boolean v) {
            for (final String id : new String[]{"submit_metrics", "submit_additional_metrics", "submit_errors"}) {
                final Checkbox c = model.findSelect(id).orElseThrow();
                c.enabled(!id.equals("submit_additional_metrics") || v).selected(v);
                c.onStateChange(model);
            }
            minecraft.setScreen(new Wrapper(model));
        }

        void close() {
            model.onClose().ifPresent(Runnable::run);
            if (minecraft.screen == this) minecraft.setScreen(null);
        }

        @Override
        public void onClose() {
            close();
        }

        @Override
        public void render(final PoseStack p, final int x, final int y, final float d) {
            renderBackground(p);
            drawCenteredString(p, font, title, width / 2, 18, 0xffffff);
            super.render(p, x, y, d);
        }
    }
}
