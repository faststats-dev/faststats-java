package dev.faststats.screen.v1_16;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Element;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public final class MinecraftScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new MinecraftText(new TextComponent(""));
    }

    @Override
    public Text translatable(final String key, final Text... args) {
        return new MinecraftText(new TranslatableComponent(key, Arrays.stream(args).map(MinecraftScreenManager::component).toArray()));
    }

    @Override
    public void openScreen(final Screen screen) {
        Minecraft.getInstance().setScreen(new ConsentScreen(screen));
    }

    @Override
    public void closeScreen(final Screen screen) {
        Minecraft.getInstance().setScreen(null);
        screen.onClose().ifPresent(Runnable::run);
    }

    private static Component component(final Text text) {
        return ((MinecraftText) text).component();
    }

    private static final class ConsentScreen extends net.minecraft.client.gui.screens.Screen {
        private static final Component DESCRIPTION = new TranslatableComponent("faststats.consent.summary");
        private static final float DISABLED_ALPHA = 0.45F;
        private static final int SPACING = 4;
        private static final int WIDGET_HEIGHT = 20;
        private static final int WIDTH = 308;

        private final Screen model;

        private ConsentScreen(final Screen model) {
            super(component(model.title()));
            this.model = model;
        }

        @Override
        protected void init() {
            final int x = (width - WIDTH) / 2;
            int y = Math.max(48, height / 2 - 70);
            for (final var element : model.bottom()) y = add(element, x, y, WIDTH) + SPACING;
            y += SPACING;
            for (final var element : model.footer()) y = add(element, x, y, WIDTH) + SPACING;

            final int linkWidth = (WIDTH - SPACING * 3) / 4;
            final int linkY = height - WIDGET_HEIGHT - SPACING;
            addLink("faststats.consent.link.info", OnboardingDefinition.INFO_URL, x, linkY, linkWidth);
            addLink("faststats.consent.link.mods", OnboardingDefinition.MODS_URL, x + linkWidth + SPACING, linkY, linkWidth);
            addLink("faststats.consent.link.privacy", OnboardingDefinition.PRIVACY_URL, x + (linkWidth + SPACING) * 2, linkY, linkWidth);
            addLink("faststats.consent.link.report", OnboardingDefinition.ABUSE_URL, x + (linkWidth + SPACING) * 3, linkY, linkWidth);
        }

        // adds the element at the given position and returns its bottom edge
        private int add(final Element<?> element, final int x, final int y, final int width) {
            if (element instanceof final Division division) {
                final List<Element<?>> elements = division.elements();
                if (elements.isEmpty()) return y;
                if (division.orientation() == Division.Orientation.VERTICAL) {
                    int bottom = y;
                    for (final var child : elements) bottom = add(child, x, bottom, width) + division.gap();
                    return bottom - division.gap();
                }
                final int childWidth = (width - division.gap() * (elements.size() - 1)) / elements.size();
                int bottom = y;
                for (int index = 0; index < elements.size(); index++) {
                    bottom = Math.max(bottom, add(elements.get(index), x + index * (childWidth + division.gap()), y, childWidth));
                }
                return bottom;
            } else if (element instanceof final Checkbox checkbox) {
                final var widget = addRenderableWidget(new net.minecraft.client.gui.components.Checkbox(x, y, width, WIDGET_HEIGHT, component(checkbox.label()), checkbox.value()) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        checkbox.selected(selected());
                        checkbox.onStateChange(model);
                        clearWidgets();
                        init();
                    }
                });
                widget.active = checkbox.enabled();
                widget.setAlpha(checkbox.enabled() ? 1.0F : DISABLED_ALPHA);
                return y + WIDGET_HEIGHT;
            } else if (element instanceof final Button button) {
                final var widget = addRenderableWidget(new net.minecraft.client.gui.components.Button(x, y, width, WIDGET_HEIGHT,
                        component(button.label()), ignored -> button.onClick(model)));
                widget.active = button.enabled();
                return y + WIDGET_HEIGHT;
            }
            return y;
        }

        private void addLink(final String key, final URI uri, final int x, final int y, final int width) {
            addRenderableWidget(new net.minecraft.client.gui.components.Button(x, y, width, WIDGET_HEIGHT, new TranslatableComponent(key), ignored -> {
                final var minecraft = this.minecraft;
                assert minecraft != null;
                minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                    if (confirmed) Util.getPlatform().openUri(uri);
                    minecraft.setScreen(this);
                }, uri.toString(), true));
            }));
        }

        @Override
        public void onClose() {
            model.close();
        }

        @Override
        public void render(final PoseStack pose, final int mouseX, final int mouseY, final float delta) {
            renderBackground(pose);
            drawCenteredString(pose, font, title, width / 2, 16, 0xFFFFFF);
            drawCenteredString(pose, font, DESCRIPTION, width / 2, 30, 0xCCCCCC);
            super.render(pose, mouseX, mouseY, delta);
        }
    }
}
