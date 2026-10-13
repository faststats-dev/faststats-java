package dev.faststats.screen.v1_19_3;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * A bordered, scrollable text area whose links are opened through the click handler.
 */
final class TextArea extends AbstractScrollWidget {
    // drawn to the right of the widget
    private static final int SCROLLBAR_WIDTH = 8;

    private final Font font;
    private final List<FormattedCharSequence> lines;
    private final Predicate<@Nullable Style> onClick;

    TextArea(final Font font, final Component text, final int x, final int y, final int width, final int maxHeight,
             final Predicate<@Nullable Style> onClick) {
        super(x, y, Math.max(1, width - SCROLLBAR_WIDTH), 0, text);
        this.font = font;
        this.lines = font.split(text, Math.max(1, getWidth() - totalInnerPadding()));
        this.onClick = onClick;
        this.height = Math.max(1, Math.min(maxHeight, getInnerHeight() + totalInnerPadding()));
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
        return lines.size() * font.lineHeight;
    }

    @Override
    protected boolean scrollbarVisible() {
        return getInnerHeight() + totalInnerPadding() > height;
    }

    @Override
    protected double scrollRate() {
        return font.lineHeight;
    }

    @Override
    protected void renderContents(final PoseStack pose, final int mouseX, final int mouseY, final float delta) {
        for (int index = 0; index < lines.size(); index++) {
            font.drawShadow(pose, lines.get(index), getX() + innerPadding(), getY() + innerPadding() + index * font.lineHeight, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && isWithin(mouseX, mouseY)
                && onClick.test(styleAt(mouseX - getX() - innerPadding(), mouseY - getY() - innerPadding() + scrollAmount()))) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    private boolean isWithin(final double x, final double y) {
        return visible && x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    private @Nullable Style styleAt(final double x, final double y) {
        if (x < 0 || y < 0) return null;
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
