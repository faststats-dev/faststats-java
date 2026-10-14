package dev.faststats.screen.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.Consumer;

import static net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.NONE;
import static net.minecraft.client.gui.GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR;

/**
 * A bordered, scrollable text area like {@link net.minecraft.client.gui.components.FittingMultiLineTextWidget}, but with clickable links.
 */
final class TextArea extends AbstractTextAreaWidget {
    private final MultiLineTextWidget text;

    TextArea(final Font font, final Component message, final int width, final int maxHeight, final Consumer<Style> onClick) {
        // the scrollbar is drawn to the right of the widget
        super(0, 0, Math.max(1, width - SCROLLBAR_WIDTH), 0, message, defaultSettings(font.lineHeight));
        this.text = new MultiLineTextWidget(message, font).setMaxWidth(getWidth() - totalInnerPadding());
        this.text.setComponentClickHandler(onClick);
        setHeight(Math.clamp(contentHeight(), 1, Math.max(1, maxHeight)));
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
