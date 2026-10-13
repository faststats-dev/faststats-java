package dev.faststats.screen.v1_20_3;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A bordered, scrollable text area whose links are opened through the click handler.
 */
final class TextArea extends AbstractScrollWidget {
    // drawn to the right of the widget
    private static final int SCROLLBAR_WIDTH = 8;

    private final Font font;
    private final Component text;
    private final MultiLineTextWidget widget;
    private final Predicate<@Nullable Style> onClick;

    TextArea(final Font font, final Component text, final int width, final int maxHeight, final Predicate<@Nullable Style> onClick) {
        // the scrollbar is drawn to the right of the widget
        super(0, 0, Math.max(1, width - SCROLLBAR_WIDTH), 0, text);
        this.font = font;
        this.text = text;
        this.widget = new MultiLineTextWidget(text, font).setMaxWidth(textWidth());
        this.onClick = onClick;
        this.height = Math.max(1, Math.min(maxHeight, widget.getHeight() + totalInnerPadding()));
    }

    // named differently from Minecraft's methods, which are remapped in production
    double scrollPosition() {
        return scrollAmount();
    }

    void scrollPosition(final double position) {
        setScrollAmount(position);
    }

    @Override
    protected int getInnerHeight() {
        return widget.getHeight();
    }

    @Override
    protected double scrollRate() {
        return font.lineHeight;
    }

    @Override
    protected void renderContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float delta) {
        widget.setX(getX() + innerPadding());
        widget.setY(getY() + innerPadding());
        widget.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && isWithin(mouseX, mouseY)
                && onClick.test(styleAt(mouseX - getX() + innerPadding(), mouseY - getY() + innerPadding() + scrollAmount()))) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    private int textWidth() {
        return Math.max(1, getWidth() - totalInnerPadding());
    }

    private boolean isWithin(final double x, final double y) {
        return visible && x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    private @Nullable Style styleAt(final double x, final double y) {
        if (x < 0 || y < 0) return null;
        final var lines = font.split(text, textWidth());
        final int index = (int) (y / font.lineHeight);
        if (index >= lines.size()) return null;

        final float[] offset = {0};
        final Style[] result = {null};
        lines.get(index).accept((position, style, codePoint) -> {
            final float characterWidth = font.width(FormattedText.of(new String(Character.toChars(codePoint)), style));
            if (x < offset[0] + characterWidth) {
                result[0] = style;
                return false;
            }
            offset[0] += characterWidth;
            return true;
        });
        return result[0];
    }
}
