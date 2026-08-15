package dev.faststats.fabric.screen;

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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class FabricScreenManager implements ScreenManager {
    private static final float DISABLED_ALPHA = 0.45F;
    private static final int BOTTOM_CONTENT_MARGIN = 10;
    private static final int BOTTOM_SECTION_GAP = 10;
    private static final int CONTENT_MARGIN = 16;
    private static final int CONTENT_MAX_WIDTH = 600;
    private static final int FOOTER_HEIGHT = 38;
    private static final int FOOTER_MAX_WIDTH = 316;
    private static final int HEADER_HEIGHT = 34;
    private static final int PANEL_BACKGROUND = 0xD0000000;
    private static final int PANEL_BORDER = 0xFFFFFFFF;
    private static final int ROOT_GAP = 16;
    private static final int SCROLLBAR_RESERVE = 20;
    private static final int SEPARATOR_COLOR = 0xFF404040;
    private static final int TEXT_PADDING_X = 24;
    private static final int TEXT_PADDING_Y = 16;

    @Override
    public Text newText() {
        return new FabricText(Component.empty());
    }

    @Override
    public Text translatable(final String text) {
        return new FabricText(Component.translatable(text));
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void closeScreen(final Screen screen) {
        Minecraft.getInstance().setScreenAndShow(null);
    }

    @Override
    public void openScreen(final Screen screen) {
        Minecraft.getInstance().setScreenAndShow(wrap(screen));
    }

    private static Component component(final Text text) {
        return ((FabricText) text).text();
    }

    private net.minecraft.client.gui.screens.Screen wrap(final Screen screen) {
        return new WrappedScreen(screen);
    }

    private static final class WrappedScreen extends net.minecraft.client.gui.screens.Screen {
        private final Screen screen;
        private final Map<Object, Double> savedScrollAmounts = new IdentityHashMap<>();
        private final Map<Object, AbstractScrollArea> scrollAreas = new IdentityHashMap<>();
        private final List<AbstractScrollArea> scrollAreasByPriority = new ArrayList<>();

        private WrappedScreen(final Screen screen) {
            super(component(screen.title()));
            this.screen = screen;
        }

        @Override
        protected void init() {
            scrollAreas.clear();
            scrollAreasByPriority.clear();

            final int topSeparator = topSeparator();
            final int bottomSeparator = bottomSeparator();
            final int contentWidth = clamp(width - CONTENT_MARGIN * 2, 1, CONTENT_MAX_WIDTH);
            final int contentX = (width - contentWidth) / 2;
            final int contentY = topSeparator + CONTENT_MARGIN;
            final int contentHeight = Math.max(1, bottomSeparator - contentY - BOTTOM_CONTENT_MARGIN);
            final int titleHeight = font.lineHeight;

            final var title = frame(width, titleHeight,
                    new StringWidget(component(screen.title()), font), 0.5F, 0.5F);
            title.setPosition(0, Math.max(0, (topSeparator - titleHeight) / 2));
            title.visitWidgets(this::addRenderableWidget);

            final Layout bottom;
            final int bottomHeight;
            if (screen.bottom().isEmpty()) {
                bottom = null;
                bottomHeight = 0;
            } else {
                bottom = buildRootDocument(screen.bottom(), contentWidth, contentHeight);
                bottomHeight = bottom.getHeight();
            }

            final int mainHeight = Math.max(1,
                    contentHeight - bottomHeight - (bottomHeight == 0 ? 0 : BOTTOM_SECTION_GAP));
            final var content = buildRootLayout(screen.elements(), contentWidth, mainHeight);
            content.setPosition(contentX, contentY);
            content.visitWidgets(this::addRenderableWidget);

            if (bottom != null) {
                bottom.setPosition(contentX, contentY + contentHeight - bottomHeight);
                bottom.visitWidgets(this::addRenderableWidget);
            }

            if (!screen.footer().isEmpty()) {
                final int footerWidth = clamp(width - CONTENT_MARGIN * 2, 1, FOOTER_MAX_WIDTH);
                final var footer = buildRootDocument(screen.footer(), footerWidth, FOOTER_HEIGHT);
                footer.setPosition((width - footerWidth) / 2,
                        bottomSeparator + Math.max(0, (height - bottomSeparator - footer.getHeight()) / 2));
                footer.visitWidgets(this::addRenderableWidget);
            }

            scrollAreas.forEach((element, area) ->
                    area.setScrollAmount(savedScrollAmounts.getOrDefault(element, 0.0)));
        }

        @Override
        public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
            graphics.fill(0, topSeparator(), width, topSeparator() + 1, SEPARATOR_COLOR);
            graphics.fill(0, bottomSeparator(), width, bottomSeparator() + 1, SEPARATOR_COLOR);
            super.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        protected void repositionElements() {
            saveScrollAmounts();
            super.repositionElements();
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
            final var rootScrollArea = scrollAreas.get(screen);
            if (rootScrollArea != null && !rootScrollArea.isMouseOver(mouseX, mouseY)) {
                return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }

            for (int index = scrollAreasByPriority.size() - 1; index >= 0; index--) {
                final var scrollArea = scrollAreasByPriority.get(index);
                if (scrollArea.maxScrollAmount() > 0 && scrollArea.isMouseOver(mouseX, mouseY)) {
                    return scrollArea.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                }
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        private void saveScrollAmounts() {
            scrollAreas.forEach((element, area) -> savedScrollAmounts.put(element, area.scrollAmount()));
        }

        private Layout buildRootLayout(final List<Element<?>> elements, final int width, final int availableHeight) {
            if (!elements.isEmpty() && elements.stream().allMatch(ScrollableTextBox.class::isInstance)) {
                return buildFlexibleTextBoxLayout(elements, width, availableHeight);
            }

            final var content = buildRootDocument(elements, width, availableHeight);
            if (content.getHeight() <= availableHeight) return content;

            scrollAreas.clear();
            scrollAreasByPriority.clear();
            final int documentWidth = scrollContentWidth(width);
            return wrapScrollable(screen, buildRootDocument(elements, documentWidth, availableHeight),
                    width, availableHeight);
        }

        private Layout buildFlexibleTextBoxLayout(final List<Element<?>> elements,
                                                  final int width, final int availableHeight) {
            final int gap = elements.size() >= 2 ? ROOT_GAP : 0;
            final int available = Math.max(0, availableHeight - gap * Math.max(0, elements.size() - 1));
            final int[] preferredHeights = new int[elements.size()];
            int preferredTotal = 0;
            for (int index = 0; index < elements.size(); index++) {
                final var textBox = (ScrollableTextBox) elements.get(index);
                preferredHeights[index] = naturalScrollableTextBoxHeight(textBox.text(), width);
                preferredTotal += preferredHeights[index];
            }

            final int[] heights = preferredTotal <= available
                    ? preferredHeights
                    : distributeCapped(available, elements, preferredHeights);
            final var layout = LinearLayout.vertical().spacing(gap);
            layout.defaultCellSetting().alignHorizontallyLeft();
            for (int index = 0; index < elements.size(); index++) {
                layout.addChild(wrapSized(elements.get(index), width, heights[index]));
            }
            layout.arrangeElements();
            return frame(width, 0, layout, 0.0F, 0.0F);
        }

        private int[] distributeCapped(final int available, final List<Element<?>> elements,
                                       final int[] preferredSizes) {
            final int[] result = new int[elements.size()];
            final boolean[] fixed = new boolean[elements.size()];
            int remaining = available;
            int remainingWeight = totalHeightWeight(elements, fixed);

            boolean changed;
            do {
                changed = false;
                for (int index = 0; index < elements.size(); index++) {
                    if (fixed[index]) continue;
                    final int weight = Math.max(1, elements.get(index).height());
                    final int share = remainingWeight == 0 ? 0
                            : (int) ((long) remaining * weight / remainingWeight);
                    if (preferredSizes[index] <= share) {
                        result[index] = preferredSizes[index];
                        remaining -= preferredSizes[index];
                        fixed[index] = true;
                        remainingWeight -= weight;
                        changed = true;
                    }
                }
            } while (changed && remainingWeight > 0);

            int assigned = 0;
            int cumulativeWeight = 0;
            for (int index = 0; index < elements.size(); index++) {
                if (fixed[index]) continue;
                cumulativeWeight += Math.max(1, elements.get(index).height());
                final int cumulativeSize = remainingWeight == 0 ? remaining
                        : (int) ((long) remaining * cumulativeWeight / remainingWeight);
                result[index] = Math.max(0, cumulativeSize - assigned);
                assigned = cumulativeSize;
            }
            return result;
        }

        private int totalHeightWeight(final List<Element<?>> elements, final boolean[] fixed) {
            int result = 0;
            for (int index = 0; index < elements.size(); index++) {
                if (!fixed[index]) result += Math.max(1, elements.get(index).height());
            }
            return result;
        }

        private Layout buildRootDocument(final List<Element<?>> elements, final int width, final int availableHeight) {
            final List<LayoutElement> children = new ArrayList<>(elements.size());
            int childrenHeight = 0;
            for (final var element : elements) {
                final var child = wrapNatural(element, percentage(width, element.width()), availableHeight);
                children.add(child);
                childrenHeight += child.getHeight();
            }

            final int gap = elements.size() >= 2 ? clamp((availableHeight - childrenHeight) / (elements.size() - 1), 0, ROOT_GAP) : 0;
            final var layout = LinearLayout.vertical().spacing(gap);
            layout.defaultCellSetting().alignHorizontallyLeft();
            children.forEach(layout::addChild);
            layout.arrangeElements();
            return frame(width, 0, layout, 0.0F, 0.0F);
        }

        private Layout buildSizedLayout(final List<Element<?>> elements, final Division.Orientation orientation,
                                        final int width, final int height, final int gap) {
            final var layout = orientation == Division.Orientation.HORIZONTAL
                    ? LinearLayout.horizontal()
                    : LinearLayout.vertical();
            layout.spacing(Math.max(0, gap));
            if (orientation == Division.Orientation.HORIZONTAL) {
                layout.defaultCellSetting().alignVerticallyMiddle();
            } else {
                layout.defaultCellSetting().alignHorizontallyCenter();
            }

            final int available = Math.max(0,
                    (orientation == Division.Orientation.HORIZONTAL ? width : height)
                            - Math.max(0, elements.size() - 1) * Math.max(0, gap));
            final int[] sizes = distribute(available, elements, orientation);
            for (int index = 0; index < elements.size(); index++) {
                final var element = elements.get(index);
                final int childWidth;
                final int childHeight;
                if (orientation == Division.Orientation.HORIZONTAL) {
                    childWidth = sizes[index];
                    childHeight = percentage(height, element.height());
                } else {
                    childWidth = percentage(width, element.width());
                    childHeight = sizes[index];
                }
                layout.addChild(wrapSized(element, childWidth, childHeight));
            }

            if (elements.isEmpty()) {
                layout.addChild(new SpacerElement(width, height));
            }
            layout.arrangeElements();
            return layout;
        }

        private LayoutElement wrapSized(final Element<?> element, int width, int height) {
            width = Math.max(1, width);
            height = Math.max(1, height);
            if (element instanceof final Division division) {
                return buildSizedLayout(division.elements(), division.orientation(), width, height, division.gap());
            } else if (element instanceof final Scrollable scrollable) {
                return wrapScrollable(element,
                        buildDocument(scrollable.elements(), width, height, scrollable.gap()), width, height);
            } else if (element instanceof final ScrollableTextBox textBox) {
                return wrapScrollableTextBox(element, textBox.text(), width, height);
            } else if (element instanceof final TextBox textBox) {
                return wrapTextBox(textBox.text(), width, height);
            } else if (element instanceof final Checkbox checkbox) {
                return frame(width, height, wrap(checkbox, width), 0.0F, 0.5F);
            } else if (element instanceof final Button button) {
                return frame(width, height, wrap(button, width, height), 0.0F, 0.5F);
            }
            throw new IllegalArgumentException("Unsupported screen element: " + element.getClass().getName());
        }

        private Layout buildDocument(final List<Element<?>> elements, final int width, final int viewportHeight, final int gap) {
            final int innerWidth = Math.max(1, width);
            final var layout = LinearLayout.vertical().spacing(Math.max(0, gap));
            layout.defaultCellSetting().alignHorizontallyLeft();
            for (final var element : elements) {
                layout.addChild(wrapNatural(element, percentage(innerWidth, element.width()), viewportHeight));
            }
            layout.arrangeElements();
            return frame(innerWidth, 0, layout, 0.0F, 0.0F);
        }

        private LayoutElement wrapNatural(final Element<?> element, int width, final int referenceHeight) {
            width = Math.max(1, width);
            if (element instanceof final Division division) {
                return buildNaturalDivision(division, width, referenceHeight);
            } else if (element instanceof final Scrollable scrollable) {
                final int height = percentage(referenceHeight, scrollable.height());
                return wrapScrollable(element,
                        buildDocument(scrollable.elements(), width, height, scrollable.gap()), width, height);
            } else if (element instanceof final ScrollableTextBox textBox) {
                final int maxHeight = percentage(referenceHeight, textBox.height());
                final int height = Math.min(maxHeight, naturalScrollableTextBoxHeight(textBox.text(), width));
                return wrapScrollableTextBox(element, textBox.text(), width, height);
            } else if (element instanceof final TextBox textBox) {
                final int innerWidth = Math.max(1, width - TEXT_PADDING_X * 2);
                final var widget = textWidget(textBox.text(), innerWidth);
                final int naturalHeight = widget.getHeight() + TEXT_PADDING_Y * 2;
                final int height = textBox.height() == 100
                        ? naturalHeight
                        : percentage(referenceHeight, textBox.height());
                return wrapTextBox(textBox.text(), width, height);
            } else if (element instanceof final Checkbox checkbox) {
                return wrap(checkbox, width);
            } else if (element instanceof final Button button) {
                return wrap(button, width, net.minecraft.client.gui.components.Button.DEFAULT_HEIGHT);
            }
            throw new IllegalArgumentException("Unsupported screen element: " + element.getClass().getName());
        }

        private Layout buildNaturalDivision(final Division division, final int width, final int referenceHeight) {
            if (division.orientation() == Division.Orientation.VERTICAL) {
                final var layout = LinearLayout.vertical().spacing(Math.max(0, division.gap()));
                layout.defaultCellSetting().alignHorizontallyLeft();
                for (final var element : division.elements()) {
                    layout.addChild(wrapNatural(element, percentage(width, element.width()), referenceHeight));
                }
                layout.arrangeElements();
                return frame(width, 0, layout, 0.0F, 0.0F);
            }

            final var layout = LinearLayout.horizontal().spacing(Math.max(0, division.gap()));
            layout.defaultCellSetting().alignVerticallyMiddle();
            final int available = Math.max(0,
                    width - Math.max(0, division.elements().size() - 1) * Math.max(0, division.gap()));
            final int[] widths = distribute(available, division.elements(), Division.Orientation.HORIZONTAL);
            for (int index = 0; index < division.elements().size(); index++) {
                final var child = wrapNatural(division.elements().get(index), widths[index], referenceHeight);
                layout.addChild(frame(widths[index], child.getHeight(), child, 0.0F, 0.5F));
            }
            layout.arrangeElements();
            return layout;
        }

        private LayoutElement wrapTextBox(final Text text, final int width, final int height) {
            final int paddingX = clamp((width - 1) / 2, 0, TEXT_PADDING_X);
            final int paddingY = textPaddingY(height);
            final var widget = textWidget(text, Math.max(1, width - paddingX * 2));
            widget.setMaxRows(Math.max(1, (height - paddingY * 2) / font.lineHeight));
            final var panel = new FrameLayout(width, height);
            panel.addChild(new PanelWidget(width, height));
            panel.addChild(widget, settings -> settings.padding(paddingX, paddingY).align(0.0F, 0.0F));
            panel.arrangeElements();
            return panel;
        }

        private LayoutElement wrapScrollableTextBox(final Element<?> element, final Text text, final int width, final int height) {
            final int panelWidth = Math.max(1, width - AbstractScrollArea.SCROLLBAR_WIDTH);
            final int paddingX = clamp((panelWidth - 1) / 2, 0, TEXT_PADDING_X);
            final int paddingY = textPaddingY(height);
            final int textWidth = Math.max(1, panelWidth - paddingX * 2);
            final int documentWidth = scrollContentWidth(width);
            final int documentPaddingLeft = Math.max(1, paddingX - SCROLLBAR_RESERVE / 2);
            final var textWidget = textWidget(text, textWidth);
            final var document = new FrameLayout(documentWidth, textWidget.getHeight() + (paddingY + 1) * 2);
            document.addChild(textWidget, settings -> settings
                    .paddingLeft(documentPaddingLeft)
                    .paddingVertical(paddingY + 1)
                    .align(0.0F, 0.0F));
            document.arrangeElements();
            final var scrollable = wrapScrollable(element, document, width, height);
            final var panel = new FrameLayout(width, height);
            panel.addChild(new PanelWidget(panelWidth, height, true, false), settings -> settings.align(0.0F, 0.0F));
            panel.addChild(scrollable, settings -> settings.align(0.0F, 0.0F));
            panel.addChild(new PanelWidget(panelWidth, height, false, true), settings -> settings.align(0.0F, 0.0F));
            panel.arrangeElements();
            return panel;
        }

        private int naturalScrollableTextBoxHeight(final Text text, final int width) {
            final int panelWidth = Math.max(1, width - AbstractScrollArea.SCROLLBAR_WIDTH);
            final int paddingX = clamp((panelWidth - 1) / 2, 0, TEXT_PADDING_X);
            final int textWidth = Math.max(1, panelWidth - paddingX * 2);
            return Math.max(1, textWidget(text, textWidth).getHeight() + (TEXT_PADDING_Y + 1) * 2);
        }

        private int textPaddingY(final int height) {
            final int spaceAroundOneLine = Math.max(0, height - font.lineHeight - 2);
            return Math.min(TEXT_PADDING_Y, spaceAroundOneLine / 2);
        }

        private MultiLineTextWidget textWidget(final Text text, final int width) {
            final var widget = new MultiLineTextWidget(component(text), font).setMaxWidth(Math.max(1, width));
            widget.setComponentClickHandler(style -> {
                if (style.getClickEvent() != null) {
                    defaultHandleClickEvent(style.getClickEvent(), minecraft, this);
                }
            });
            widget.active = true;
            return widget;
        }

        private net.minecraft.client.gui.components.Checkbox wrap(final Checkbox checkbox, final int width) {
            final var widget = net.minecraft.client.gui.components.Checkbox.builder(component(checkbox.label()), font)
                    .maxWidth(Math.max(1, width))
                    .onValueChange((self, value) -> {
                        checkbox.selected(value);
                        checkbox.onStateChange(screen);
                        screenExecutor.execute(() -> {
                            saveScrollAmounts();
                            rebuildWidgets();
                        });
                    })
                    .selected(checkbox.enabled() && checkbox.selected())
                    .build();
            widget.active = checkbox.enabled();
            widget.setAlpha(checkbox.enabled() ? 1.0F : DISABLED_ALPHA);
            return widget;
        }

        private net.minecraft.client.gui.components.Button wrap(final Button button, final int width, final int height) {
            final var widget = net.minecraft.client.gui.components.Button.builder(component(button.label()), ignored -> button.onClick(screen))
                    .size(Math.max(1, width), Math.max(1, height))
                    .build();
            widget.active = button.enabled();
            return widget;
        }

        private ScrollableLayout wrapScrollable(final Object element, final Layout content, final int width, final int height) {
            final var scrollable = new ScrollableLayout(minecraft, content, Math.max(1, height));
            scrollable.setMinWidth(scrollContentWidth(width));
            scrollable.setMinHeight(Math.max(1, height));
            scrollable.setMaxHeight(Math.max(1, height));
            scrollable.arrangeElements();
            scrollable.visitWidgets(widget -> {
                if (widget instanceof final AbstractScrollArea scrollArea) {
                    scrollAreas.put(element, scrollArea);
                    scrollAreasByPriority.add(0, scrollArea);
                }
            });
            return scrollable;
        }

        private FrameLayout frame(final int width, final int height, final LayoutElement child, final float alignX, final float alignY) {
            final var frame = new FrameLayout(Math.max(1, width), Math.max(1, height));
            frame.addChild(child, settings -> settings.align(alignX, alignY));
            frame.arrangeElements();
            return frame;
        }

        private int[] distribute(final int available, final List<Element<?>> elements, final Division.Orientation orientation) {
            final int[] result = new int[elements.size()];
            if (elements.isEmpty()) return result;

            long totalWeight = 0;
            for (final var element : elements) {
                totalWeight += Math.max(0,
                        orientation == Division.Orientation.HORIZONTAL ? element.width() : element.height());
            }
            final boolean useEqualWeights = totalWeight == 0;
            if (useEqualWeights) totalWeight = elements.size();

            int assigned = 0;
            long cumulativeWeight = 0;
            for (int index = 0; index < elements.size(); index++) {
                final int weight = Math.max(0,
                        orientation == Division.Orientation.HORIZONTAL
                                ? elements.get(index).width()
                                : elements.get(index).height());
                cumulativeWeight += useEqualWeights ? 1 : weight;
                final int cumulativeSize = (int) (available * cumulativeWeight / totalWeight);
                result[index] = Math.max(0, cumulativeSize - assigned);
                assigned = cumulativeSize;
            }
            return result;
        }

        private int percentage(final int available, final int percentage) {
            return Math.max(1, (int) ((long) Math.max(0, available) * Math.max(0, percentage) / 100));
        }

        private int scrollContentWidth(final int width) {
            return Math.max(1, width - SCROLLBAR_RESERVE);
        }

        private int topSeparator() {
            return clamp(height / 3, 1, HEADER_HEIGHT);
        }

        private int bottomSeparator() {
            return Math.max(topSeparator() + 1, height - clamp(height / 3, 1, FOOTER_HEIGHT));
        }

        public static int clamp(final long value, final int min, final int max) {
            if (min > max) throw new IllegalArgumentException(min + " > " + max);
            return (int) Math.min(max, Math.max(value, min));
        }

        private static final class PanelWidget extends AbstractWidget {
            private final boolean background;
            private final boolean border;

            private PanelWidget(final int width, final int height) {
                this(width, height, true, true);
            }

            private PanelWidget(final int width, final int height, final boolean background, final boolean border) {
                super(0, 0, Math.max(1, width), Math.max(1, height), Component.empty());
                this.background = background;
                this.border = border;
                active = false;
            }

            @Override
            protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
                if (background) {
                    graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, PANEL_BACKGROUND);
                }
                if (border) graphics.outline(getX(), getY(), getWidth(), getHeight(), PANEL_BORDER);
            }

            @Override
            protected void updateWidgetNarration(final NarrationElementOutput output) {
            }
        }
    }
}
