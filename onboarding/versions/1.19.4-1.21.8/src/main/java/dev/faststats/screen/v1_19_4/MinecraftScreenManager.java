package dev.faststats.screen.v1_19_4;

import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Element;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.List;

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
        Minecraft.getInstance().setScreen(null);
        screen.onClose().ifPresent(Runnable::run);
    }

    private static Component component(final Text text) {
        return ((MinecraftText) text).component();
    }

    private static final class ConsentScreen extends net.minecraft.client.gui.screens.Screen {
        private static final Component DESCRIPTION = Component.literal("Anonymous metrics and errors help mod developers improve their projects.");
        private static final float DISABLED_ALPHA = 0.45F;
        private static final int FOOTER_HEIGHT = 33;
        private static final int SPACING = 4;
        private static final int WIDTH = 308;

        private final Screen model;

        private ConsentScreen(final Screen model) {
            super(component(model.title()));
            this.model = model;
        }

        @Override
        protected void init() {
            final var header = LinearLayout.vertical().spacing(SPACING);
            header.defaultCellSetting().alignHorizontallyCenter();
            header.addChild(new StringWidget(title, font));
            header.addChild(new MultiLineTextWidget(DESCRIPTION, font).setMaxWidth(width - 16).setCentered(true));
            header.arrangeElements();
            final var layout = new HeaderAndFooterLayout(this, header.getHeight() + 16, FOOTER_HEIGHT);
            layout.addToHeader(header);

            final var content = layout.addToContents(LinearLayout.vertical().spacing(SPACING));
            content.defaultCellSetting().alignHorizontallyLeft();
            model.bottom().forEach(element -> add(content, element, WIDTH));
            final var links = content.addChild(LinearLayout.horizontal().spacing(SPACING));
            final int linkWidth = (WIDTH - SPACING * 3) / 4;
            addLink(links, "Info", OnboardingDefinition.INFO_URL, linkWidth);
            addLink(links, "Mods", OnboardingDefinition.MODS_URL, linkWidth);
            addLink(links, "Privacy", OnboardingDefinition.PRIVACY_URL, linkWidth);
            addLink(links, "Report", OnboardingDefinition.ABUSE_URL, linkWidth);

            final var footer = layout.addToFooter(LinearLayout.vertical().spacing(SPACING));
            model.footer().forEach(element -> add(footer, element, WIDTH));

            layout.visitWidgets(this::addRenderableWidget);
            layout.arrangeElements();
        }

        private void add(final LinearLayout parent, final Element<?> element, final int width) {
            if (element instanceof final Division division) {
                final List<Element<?>> elements = division.elements();
                final boolean horizontal = division.orientation() == Division.Orientation.HORIZONTAL;
                final var layout = parent.addChild((horizontal ? LinearLayout.horizontal() : LinearLayout.vertical()).spacing(division.gap()));
                layout.defaultCellSetting().alignHorizontallyLeft();
                final int childWidth = horizontal && !elements.isEmpty()
                        ? (width - division.gap() * (elements.size() - 1)) / elements.size()
                        : width;
                elements.forEach(child -> add(layout, child, childWidth));
            } else if (element instanceof final Checkbox checkbox) {
                final var widget = parent.addChild(net.minecraft.client.gui.components.Checkbox.builder(component(checkbox.label()), font)
                        .selected(checkbox.value())
                        .onValueChange((self, value) -> {
                            checkbox.selected(value);
                            checkbox.onStateChange(model);
                            rebuildWidgets();
                        })
                        .build());
                widget.active = checkbox.enabled();
                widget.setAlpha(checkbox.enabled() ? 1.0F : DISABLED_ALPHA);
            } else if (element instanceof final Button button) {
                final var widget = parent.addChild(net.minecraft.client.gui.components.Button.builder(component(button.label()),
                        ignored -> button.onClick(model)).width(width).build());
                widget.active = button.enabled();
            }
        }

        private void addLink(final LinearLayout parent, final String label, final URI uri, final int width) {
            parent.addChild(net.minecraft.client.gui.components.Button.builder(Component.literal(label),
                    ConfirmLinkScreen.confirmLink(this, uri.toString())).width(width).build());
        }

        @Override
        public void onClose() {
            model.close();
        }
    }
}
