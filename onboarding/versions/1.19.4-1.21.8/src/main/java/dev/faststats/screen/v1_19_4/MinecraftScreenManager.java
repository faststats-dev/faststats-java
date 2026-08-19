package dev.faststats.screen.v1_19_4;

import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;

public final class MinecraftScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new MinecraftText(Component.empty());
    }

    @Override
    public Text translatable(final String key) {
        return new MinecraftText(Component.translatable(key));
    }

    @Override
    public void openScreen(final Screen screen) {
        Minecraft.getInstance().setScreen(new ConsentScreen(screen));
    }

    @Override
    public void closeScreen(final Screen screen) {
        screen.onClose().ifPresent(Runnable::run);
        if (Minecraft.getInstance().screen instanceof final ConsentScreen wrapper && wrapper.model == screen)
            Minecraft.getInstance().setScreen(null);
    }

    private static final class ConsentScreen extends net.minecraft.client.gui.screens.Screen {
        private final Screen model;

        private ConsentScreen(final Screen model) {
            super(((MinecraftText) model.title()).component());
            this.model = model;
        }

        @Override
        protected void init() {
            final int left = Math.max(8, width / 2 - 150);
            final int w = Math.min(300, width - 16);
            int y = Math.max(48, height / 2 - 70);
            addToggle("submit_metrics", left, y, w);
            y += 24;
            addToggle("submit_additional_metrics", left, y, w);
            y += 24;
            addToggle("submit_errors", left, y, w);
            y += 28;
            addRenderableWidget(Button.builder(Component.literal("Unselect All"), ignored -> setAll(false)).bounds(left, y, (w - 4) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Select All"), ignored -> setAll(true)).bounds(left + (w + 4) / 2, y, (w - 4) / 2, 20).build());
            y += 24;
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> close()).bounds(left, y, w, 20).build());
            final int linkWidth = Math.max(45, (w - 6) / 4);
            addLink("Info", URI.create("https://faststats.dev/info"), left, height - 24, linkWidth);
            addLink("Mods", URI.create("https://faststats.dev/mods"), left + linkWidth + 2, height - 24, linkWidth);
            addLink("Privacy", URI.create("https://faststats.dev/privacy"), left + (linkWidth + 2) * 2, height - 24, linkWidth);
            addLink("Report", URI.create("https://faststats.dev/abuse"), left + (linkWidth + 2) * 3, height - 24, linkWidth);
        }

        private void addToggle(final String id, final int x, final int y, final int width) {
            final Checkbox checkbox = model.findSelect(id).orElseThrow();
            final Button widget = Button.builder(label(checkbox), ignored -> {
                checkbox.selected(!checkbox.value());
                checkbox.onStateChange(model);
                rebuildWidgets();
            }).bounds(x, y, width, 20).build();
            widget.active = checkbox.enabled();
            addRenderableWidget(widget);
        }

        private Component label(final Checkbox checkbox) {
            return Component.literal(checkbox.value() ? "[x] " : "[ ] ").append(((MinecraftText) checkbox.label()).component());
        }

        private void setAll(final boolean selected) {
            final Checkbox metrics = model.findSelect("submit_metrics").orElseThrow();
            metrics.selected(selected);
            metrics.onStateChange(model);
            final Checkbox additional = model.findSelect("submit_additional_metrics").orElseThrow();
            additional.enabled(selected).selected(selected);
            additional.onStateChange(model);
            final Checkbox errors = model.findSelect("submit_errors").orElseThrow();
            errors.selected(selected);
            errors.onStateChange(model);
            rebuildWidgets();
        }

        private void addLink(final String label, final URI uri, final int x, final int y, final int width) {
            addRenderableWidget(Button.builder(Component.literal(label), ignored -> {
                if (Desktop.isDesktopSupported()) try {
                    Desktop.getDesktop().browse(uri);
                } catch (final Exception ignoredException) {
                }
            }).bounds(x, y, width, 20).build());
        }

        private void close() {
            MinecraftScreenManager.thisClose(model);
        }

        @Override
        public void onClose() {
            close();
        }

        @Override
        public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float delta) {
            renderBackground(graphics, mouseX, mouseY, delta);
            graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
            graphics.drawCenteredString(font, "Anonymous metrics and errors help mod developers improve their projects.", width / 2, 34, 0xCCCCCC);
            super.render(graphics, mouseX, mouseY, delta);
        }
    }

    private static void thisClose(final Screen model) {
        model.onClose().ifPresent(Runnable::run);
        if (Minecraft.getInstance().screen instanceof ConsentScreen) Minecraft.getInstance().setScreen(null);
    }
}
