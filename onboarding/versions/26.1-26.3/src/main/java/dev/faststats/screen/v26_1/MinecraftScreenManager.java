package dev.faststats.screen.v26_1;

import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Division;
import dev.faststats.screen.Element;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Scrollable;
import dev.faststats.screen.ScrollableTextBox;
import dev.faststats.screen.Text;
import dev.faststats.screen.TextBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.NONE;
import static net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR;

public final class MinecraftScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new MinecraftText(Component.empty());
    }

    @Override
    public Text translatable(final String text) {
        return new MinecraftText(Component.translatable(text));
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void closeScreen(final Screen screen) {
        Minecraft.getInstance().setScreenAndShow(null);
        screen.onClose().ifPresent(Runnable::run);
    }

    @Override
    public void openScreen(final Screen screen) {
        Minecraft.getInstance().setScreenAndShow(new WrappedScreen(screen));
    }

    private static Component component(final Text text) {
        return ((MinecraftText) text).text();
    }

    private static final class WrappedScreen extends net.minecraft.client.gui.screens.Screen {
        private static final float DISABLED_ALPHA = 0.45F;
        private static final int CONTENT_MAX_WIDTH = 600;
        private static final int FOOTER_MAX_WIDTH = 308;
        private static final int MARGIN = 20;
        private static final int SCROLLBAR_RESERVE = 20;
        private static final int SPACING = 8;

        private final Screen screen;
        private final Map<Object, AbstractScrollArea> scrollAreas = new IdentityHashMap<>();
        private final Map<Object, Double> scrollAmounts = new IdentityHashMap<>();

        private WrappedScreen(final Screen screen) {
            super(component(screen.title()));
            this.screen = screen;
        }

        @Override
        protected void init() {
            final var layout = new HeaderAndFooterLayout(this);
            layout.addTitleHeader(title, font);

            if (!screen.footer().isEmpty()) {
                final var footer = column(screen.footer(), Math.min(width - MARGIN * 2, FOOTER_MAX_WIDTH), 0);
                layout.addToFooter(footer);
                layout.setFooterHeight(Math.max(HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT,
                        footer.getHeight() + HeaderAndFooterLayout.MAGIC_PADDING));
            }

            final int contentWidth = Math.min(width - MARGIN * 2, CONTENT_MAX_WIDTH);
            final int available = layout.getContentHeight() - SPACING * 2;
            final var bottom = column(screen.bottom(), contentWidth, available);
            final int mainHeight = available - (screen.bottom().isEmpty() ? 0 : bottom.getHeight() + SPACING);

            Layout main = column(screen.elements(), contentWidth, mainHeight);
            if (main.getHeight() > mainHeight) {
                main = scrollable(screen, column(screen.elements(), contentWidth - SCROLLBAR_RESERVE, mainHeight), contentWidth, mainHeight);
            }

            final var content = LinearLayout.vertical().spacing(SPACING);
            content.addChild(main);
            if (!screen.bottom().isEmpty()) content.addChild(bottom);
            layout.addToContents(content, settings -> settings.paddingBottom(SPACING));

            layout.visitWidgets(this::addRenderableWidget);
            layout.arrangeElements();
            scrollAreas.forEach((element, area) -> area.setScrollAmount(scrollAmounts.getOrDefault(element, 0.0)));

            final boolean inWorld = minecraft.level != null;
            final var header = separator(inWorld ? INWORLD_HEADER_SEPARATOR : HEADER_SEPARATOR);
            final var footer = separator(inWorld ? INWORLD_FOOTER_SEPARATOR : FOOTER_SEPARATOR);
            header.setPosition(0, layout.getHeaderHeight() - 2);
            footer.setPosition(0, height - layout.getFooterHeight());
        }

        private ImageWidget separator(final Identifier texture) {
            return addRenderableOnly(ImageWidget.texture(width, 2, texture, 32, 2));
        }

        @Override
        protected void rebuildWidgets() {
            scrollAreas.forEach((element, area) -> scrollAmounts.put(element, area.scrollAmount()));
            scrollAreas.clear();
            super.rebuildWidgets();
        }

        @Override
        public void onClose() {
            screen.close();
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
            return scroll(children(), mouseX, mouseY, scrollX, scrollY)
                    || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // routes the scroll to the innermost scroll area under the mouse that can still scroll
        private static boolean scroll(final List<? extends GuiEventListener> children, final double mouseX, final double mouseY,
                                      final double scrollX, final double scrollY) {
            for (final var child : children) {
                if (!child.isMouseOver(mouseX, mouseY)) continue;
                if (child instanceof final ContainerEventHandler container
                        && scroll(container.children(), mouseX, mouseY, scrollX, scrollY)) return true;
                return child instanceof final AbstractScrollArea area && area.maxScrollAmount() > 0
                        && area.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            return false;
        }

        private Layout column(final List<Element<?>> elements, final int width, final int height) {
            final var column = LinearLayout.vertical().spacing(SPACING);
            final int referenceHeight = height - SPACING * Math.max(0, elements.size() - 1);
            for (final var element : elements) {
                column.addChild(build(element, percentage(width, element.width()), referenceHeight));
            }
            column.arrangeElements();
            return column;
        }

        private LayoutElement build(final Element<?> element, final int width, final int height) {
            return switch (element) {
                case final Division division -> division(division, width, height);
                case final Scrollable scrollable -> {
                    final int scrollableHeight = percentage(height, scrollable.height());
                    final var content = LinearLayout.vertical().spacing(scrollable.gap());
                    for (final var child : scrollable.elements()) {
                        content.addChild(build(child, percentage(width - SCROLLBAR_RESERVE, child.width()), scrollableHeight));
                    }
                    content.arrangeElements();
                    yield scrollable(element, content, width, scrollableHeight);
                }
                case final ScrollableTextBox textBox ->
                        textArea(element, textBox.text(), width, percentage(height, textBox.height()));
                case final TextBox textBox ->
                        textArea(element, textBox.text(), width, percentage(height, textBox.height()));
                case final Checkbox checkbox -> checkbox(checkbox, width);
                case final Button button -> {
                    final var widget = net.minecraft.client.gui.components.Button.builder(component(button.label()),
                            ignored -> button.onClick(screen)).width(width).build();
                    widget.active = button.enabled();
                    yield widget;
                }
                case null, default -> throw new IllegalArgumentException(
                        "Unsupported screen element: " + element.getClass().getName()
                );
            };
        }

        private Layout division(final Division division, final int width, final int height) {
            final var elements = division.elements();
            if (division.orientation() == Division.Orientation.VERTICAL) {
                final var layout = LinearLayout.vertical().spacing(division.gap());
                elements.forEach(element -> layout.addChild(build(element, percentage(width, element.width()), height)));
                return layout;
            }

            final var layout = LinearLayout.horizontal().spacing(division.gap());
            layout.defaultCellSetting().alignVerticallyMiddle();
            final int available = width - division.gap() * Math.max(0, elements.size() - 1);
            final int totalWeight = elements.stream().mapToInt(Element::width).sum();
            elements.forEach(element -> layout.addChild(build(element,
                    totalWeight == 0 ? available / elements.size() : available * element.width() / totalWeight,
                    percentage(height, element.height()))));
            return layout;
        }

        private TextArea textArea(final Element<?> element, final Text text, final int width, final int maxHeight) {
            final var area = new TextArea(component(text), width, maxHeight);
            scrollAreas.put(element, area);
            return area;
        }

        private net.minecraft.client.gui.components.Checkbox checkbox(final Checkbox checkbox, final int width) {
            final var widget = net.minecraft.client.gui.components.Checkbox.builder(component(checkbox.label()), font)
                    .maxWidth(width)
                    .selected(checkbox.value())
                    .onValueChange((self, value) -> {
                        checkbox.selected(value);
                        checkbox.onStateChange(screen);
                        screenExecutor.execute(this::rebuildWidgets);
                    })
                    .build();
            widget.active = checkbox.enabled();
            widget.setAlpha(checkbox.enabled() ? 1.0F : DISABLED_ALPHA);
            return widget;
        }

        private ScrollableLayout scrollable(final Object element, final Layout content, final int width, final int height) {
            final var scrollable = new ScrollableLayout(minecraft, content, height);
            scrollable.setMinWidth(width - SCROLLBAR_RESERVE);
            scrollable.arrangeElements();
            scrollable.visitWidgets(widget -> {
                if (widget instanceof final AbstractScrollArea area) scrollAreas.put(element, area);
            });
            return scrollable;
        }

        private static int percentage(final int available, final int percentage) {
            return Math.max(1, available * percentage / 100);
        }

        // a bordered, scrollable text area like FittingMultiLineTextWidget, but with clickable links
        private final class TextArea extends AbstractTextAreaWidget {
            private final MultiLineTextWidget text;

            private TextArea(final Component message, final int width, final int maxHeight) {
                super(0, 0, Math.max(1, width - SCROLLBAR_WIDTH), 0, message, defaultSettings(font.lineHeight));
                this.text = new MultiLineTextWidget(message, font).setMaxWidth(getWidth() - totalInnerPadding());
                this.text.setComponentClickHandler(style -> {
                    if (style.getClickEvent() != null)
                        defaultHandleClickEvent(style.getClickEvent(), minecraft, WrappedScreen.this);
                });
                setHeight(Math.clamp(contentHeight(), 1, maxHeight));
            }

            @Override
            public void setX(final int x) {
                super.setX(x);
                text.setX(getInnerLeft());
            }

            @Override
            public void setY(final int y) {
                super.setY(y);
                text.setY(getInnerTop());
            }

            @Override
            protected int getInnerHeight() {
                return text.getHeight();
            }

            @Override
            protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
                text.visitLines(graphics.textRendererForWidget(this, isHovered() ? TOOLTIP_AND_CURSOR : NONE));
            }

            @Override
            public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
                text.onClick(new MouseButtonEvent(event.x(), event.y() + scrollAmount(), event.buttonInfo()), doubleClick);
            }

            @Override
            protected void updateWidgetNarration(final NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, getMessage());
            }
        }
    }
}
