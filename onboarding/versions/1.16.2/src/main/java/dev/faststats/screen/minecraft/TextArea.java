package dev.faststats.screen.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Predicate;

/**
 * A bordered, scrollable text area whose links are opened through the click handler.
 * <p>
 * This version has no scrollable widget to build upon, so it is drawn by hand.
 */
final class TextArea extends AbstractWidget {
    private static final int BORDER_COLOR = 0xFFA0A0A0;
    private static final int PADDING = 4;
    // drawn to the right of the widget
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_SPACING = 2;

    private final Font font;
    private final List<FormattedCharSequence> lines;
    private final Predicate<@Nullable Style> onClick;
    private double scroll;

    TextArea(final Font font, final Component text, final int x, final int y, final int width, final int maxHeight,
             final Predicate<@Nullable Style> onClick) {
        super(x, y, Math.max(1, width - SCROLLBAR_WIDTH - SCROLLBAR_SPACING), 0, text);
        this.font = font;
        this.lines = font.split(text, Math.max(1, this.width - PADDING * 2));
        this.onClick = onClick;
        this.height = Math.max(1, Math.min(maxHeight, contentHeight()));
    }

    // named differently from Minecraft's methods, which are remapped in production
    double scrollPosition() {
        return scroll;
    }

    void scrollPosition(final double position) {
        scroll = Math.max(0, Math.min(maxScroll(), position));
    }

    @Override
    public void renderButton(final PoseStack pose, final int mouseX, final int mouseY, final float delta) {
        fill(pose, x, y, x + width, y + height, BORDER_COLOR);
        fill(pose, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000);

        scissor(x + 1, y + 1, x + width - 1, y + height - 1);
        for (int index = 0; index < lines.size(); index++) {
            final int lineY = y + PADDING + index * font.lineHeight - (int) scroll;
            if (lineY + font.lineHeight < y) continue;
            if (lineY > y + height) break;
            font.drawShadow(pose, lines.get(index), x + PADDING, lineY, 0xFFFFFF);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll() <= 0) return;
        final int left = x + width + SCROLLBAR_SPACING;
        final int barHeight = Math.max(8, height * height / contentHeight());
        final int barY = y + (int) ((height - barHeight) * scroll / maxScroll());
        fill(pose, left, y, left + SCROLLBAR_WIDTH, y + height, 0xFF000000);
        fill(pose, left, barY, left + SCROLLBAR_WIDTH, barY + barHeight, 0xFF808080);
        fill(pose, left, barY, left + SCROLLBAR_WIDTH - 1, barY + barHeight - 1, 0xFFC0C0C0);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double amount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollPosition(scroll - amount * font.lineHeight * 3);
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        return onClick.test(styleAt(mouseX - x - PADDING, mouseY - y - PADDING + scroll));
    }

    private int contentHeight() {
        return lines.size() * font.lineHeight + PADDING * 2;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private static void scissor(final int left, final int top, final int right, final int bottom) {
        final var window = Minecraft.getInstance().getWindow();
        final double scale = window.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (left * scale), (int) (window.getHeight() - bottom * scale),
                (int) ((right - left) * scale), (int) ((bottom - top) * scale));
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
