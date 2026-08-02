package com.example.mixin;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@NullMarked
@Mixin(TitleScreen.class)
public abstract class OnboardingScreen {
    @Unique
    private static boolean dismissed;

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void showFastStatsOnboardingScreen(final CallbackInfo cir) {
        if (!dismissed) {
            OnboardingDefinition.create().open();
            // Minecraft.getInstance().setScreenAndShow(new Onboarding());
        }
    }

    // todo: add a button to open the onboarding screen somewhere in the settings

    private static final class Onboarding extends Screen {
        private static final String MODS_URL = "https://faststats.dev/mods";
        private static final String ABUSE_URL = "https://faststats.dev/abuse";
        private static final String INFO_URL = "https://faststats.dev/info";
        private static final Component title = Component.literal("FastStats Metrics");
        private static final Component description = Component.literal("")
                .append(link("FastStats", MODS_URL))
                .append(" collects anonymous usage statistics and errors.\n")
                .append("Keeping Metrics and Error tracking enabled helps developers to improve their mods.\n\n")
                .append("If you suspect a developer is collecting personal data or bypassing any opt-out option,\n")
                .append("please report it at: ")
                .append(link(ABUSE_URL, ABUSE_URL));

        private static final Component submitMetrics = Component.literal("Submit Metrics");
        private static final Component submitAdditionalMetrics = Component.literal("Submit Additional Metrics (provided by the developer)");
        private static final Component errorTracking = Component.literal("Submit Errors");

        private static final int REFERENCE_WIDTH = 1280;
        private static final int REFERENCE_HEIGHT = 720;
        private static final int MAX_PANEL_WIDTH = 1186;
        private static final int PANEL_MARGIN = 27;
        private static final int PANEL_SIDE_MARGIN = 16;
        private static final int TITLE_OFFSET = 45;
        private static final int SCROLL_TOP_OFFSET = 88;
        private static final int CONTENT_TOP_OFFSET = 122;
        private static final int BOX_PADDING_X = 24;
        private static final int BOX_PADDING_Y = 16;
        private static final int BOX_GAP = 12;
        private static final int CHECKBOX_GAP = 30;
        private static final int CHECKBOX_TOP_GAP = 22;
        private static final int BUTTON_TOP_GAP = 16;
        private static final int BUTTON_GAP = 2;
        private static final int BUTTON_HEIGHT = 20;

        private static final String[] alwaysCollectedData = new String[]{
                "Mod version",
                "Fabric version",
                "..."
        };

        private static final Set<String> expandedDetails = new HashSet<>();

        private static final Set<Mod> mods = Set.of(
                new Mod("TreeHugger69", new String[]{"Default Metrics", "Error Tracking"}, new String[]{"client_age", "language"}),
                new Mod("Sucker123", new String[]{"Default Metrics"}, new String[]{})
        );

        private record Mod(String name, String[] metrics, String[] additionalMetrics) {

        }

        private boolean collectedDataExpanded;
        private boolean installedModsExpanded;
        private boolean submitMetricsSelected = true;
        private int scrollOffset;

        private Checkbox submitMetricsWidget;
        private Checkbox submitAdditionalMetricsWidget;
        private Checkbox errorTrackingWidget;
        private Button declineButton;
        private Button acceptButton;

        private Onboarding() {
            super(title);
        }

        @Override
        protected void init() {
            var submitAdditionalMetricsSelected = new AtomicBoolean(true);
            var errorTrackingSelected = new AtomicBoolean(true);

            this.scrollOffset = Math.min(this.scrollOffset, this.maxScrollOffset());

            this.submitMetricsWidget = this.addRenderableWidget(Checkbox.builder(Onboarding.submitMetrics, this.font)
                    .pos(contentLeft(), checkboxY())
                    .onValueChange((checkbox, selected) -> {
                        this.submitMetricsSelected = selected;
                        this.rebuildWidgets();
                    })
                    .selected(this.submitMetricsSelected)
                    .build());

            this.submitAdditionalMetricsWidget = this.addRenderableWidget(Checkbox.builder(this.additionalMetricsLabel(), this.font)
                    .pos(contentLeft(), checkboxY() + scaled(CHECKBOX_GAP))
                    .onValueChange((checkbox, selected) -> submitAdditionalMetricsSelected.set(selected))
                    .selected(submitAdditionalMetricsSelected.get() && this.submitMetricsSelected)
                    .build());
            this.submitAdditionalMetricsWidget.active = this.submitMetricsSelected;
            this.submitAdditionalMetricsWidget.setAlpha(this.submitMetricsSelected ? 1.0F : 0.45F);
            this.errorTrackingWidget = this.addRenderableWidget(Checkbox.builder(Onboarding.errorTracking, this.font)
                    .pos(contentLeft(), checkboxY() + scaled(CHECKBOX_GAP) * 2)
                    .onValueChange((checkbox, selected) -> errorTrackingSelected.set(selected))
                    .selected(errorTrackingSelected.get())
                    .build());

            final var buttonGap = scaled(BUTTON_GAP);
            final var buttonWidth = (contentWidth() - buttonGap) / 2;
            this.declineButton = this.addRenderableWidget(Button.builder(Component.literal("Decline All").withColor(TextColor.RED), button -> {
                // todo: disable all… close immediately or let the user also click "confirm"?
                this.onClose();
            }).bounds(contentLeft(), buttonY(), buttonWidth, buttonHeight()).build());
            this.acceptButton = this.addRenderableWidget(Button.builder(Component.literal("Confirm Selection").withColor(TextColor.GREEN), button -> {
                // todo: save selection
                this.onClose();
            }).bounds(contentLeft() + buttonWidth + buttonGap, buttonY(), buttonWidth, buttonHeight()).build());
            this.updateWidgetPositions();
        }

        @Override
        public void onClose() {
            dismissed = true;
            Minecraft.getInstance().setScreenAndShow(new TitleScreen());
        }

        // fixme: this is hellish code, i bet i will forget what it does until tomorrow :)
        @Override
        public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
            super.extractRenderState(graphics, mouseX, mouseY, a);

            final int panelWidth = panelWidth();
            final int panelHeight = panelHeight();
            final int panelX = panelX();
            final int panelY = panelY();
            final int border = 0xFFFFFFFF;
            final int text = 0xFFE0E0E0;

            graphics.outline(panelX, panelY, panelWidth, panelHeight, border);
            graphics.centeredText(this.font, title, this.width / 2, panelY + scaled(TITLE_OFFSET), text);

            final int contentInset = contentInset(panelWidth);
            final int descriptionX = panelX + contentInset;
            final int descriptionY = descriptionY();
            final int descriptionWidth = panelWidth - contentInset * 2;
            final int descriptionHeight = descriptionHeight();
            graphics.enableScissor(panelX + 1, scrollTop(), panelX + panelWidth - 1, scrollBottom());
            graphics.fill(descriptionX + 1, descriptionY + 1, descriptionX + descriptionWidth - 1, descriptionY + descriptionHeight - 1, 0xFF000000);
            graphics.outline(descriptionX, descriptionY, descriptionWidth, descriptionHeight, border);
            graphics.textWithWordWrap(this.font, description, descriptionX + scaled(BOX_PADDING_X), descriptionY + scaled(BOX_PADDING_Y), descriptionTextWidth(), text, false);

            final int infoY = descriptionY + descriptionHeight + scaled(BOX_GAP);
            final int infoHeight = infoHeight();
            graphics.fill(descriptionX + 1, infoY + 1, descriptionX + descriptionWidth - 1, infoY + infoHeight - 1, 0xFF000000);
            graphics.outline(descriptionX, infoY, descriptionWidth, infoHeight, border);
            graphics.textWithWordWrap(this.font, infoDescription(), descriptionX + scaled(BOX_PADDING_X), infoY + scaled(BOX_PADDING_Y), descriptionTextWidth(), text, false);
            graphics.disableScissor();
            this.renderScrollbar(graphics, panelX, panelY, panelWidth, panelHeight, border);
            if (this.isHoveringClickableText(mouseX, mouseY)) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        // fixme: all of this sucks
        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            final Style clickedStyle = this.getStyleAt(description, event.x(), event.y(), descriptionTextX(), descriptionTextY(), descriptionTextWidth());
            if (clickedStyle != null && clickedStyle.getClickEvent() != null) {
                defaultHandleClickEvent(clickedStyle.getClickEvent(), this.minecraft, this);
                return true;
            }

            if (event.y() < scrollTop() || event.y() > scrollBottom()) {
                return super.mouseClicked(event, doubleClick);
            }

            final Component info = infoDescription();
            final Style clickedInfoStyle = this.getStyleAt(info, event.x(), event.y(), infoTextX(), infoTextY(), descriptionTextWidth());
            if (clickedInfoStyle != null && clickedInfoStyle.getClickEvent() != null) {
                defaultHandleClickEvent(clickedInfoStyle.getClickEvent(), this.minecraft, this);
                return true;
            }

            final String clickedInfoLine = this.getLineAt(info, event.x(), event.y(), infoTextX(), infoTextY(), descriptionTextWidth());
            if (clickedInfoLine != null) {
                if (clickedInfoLine.contains("What data is collected?")) {
                    this.collectedDataExpanded = !this.collectedDataExpanded;
                    this.updateWidgetPositions();
                    return true;
                }
                if (clickedInfoLine.contains("List of installed mods that use FastStats")) {
                    this.installedModsExpanded = !this.installedModsExpanded;
                    this.updateWidgetPositions();
                    return true;
                }
                if (clickedInfoLine.contains("Additional Metrics")) {
                    expandedDetails.add(""); // todo: idk how to expand that crap
                    this.updateWidgetPositions();
                    return true;
                }
            }

            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
            final int maxScrollOffset = this.maxScrollOffset();
            if (maxScrollOffset <= 0) {
                return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }

            final var b = this.scrollOffset - (int) (verticalAmount * 20);
            this.scrollOffset = Math.clamp(b, 0, maxScrollOffset);
            this.updateWidgetPositions();
            return true;
        }

        private boolean isHoveringClickableText(final double mouseX, final double mouseY) {
            if (mouseY < scrollTop() || mouseY > scrollBottom()) {
                return false;
            }

            final Style descriptionStyle = this.getStyleAt(description, mouseX, mouseY, descriptionTextX(), descriptionTextY(), descriptionTextWidth());
            if (descriptionStyle != null && descriptionStyle.getClickEvent() != null) {
                return true;
            }

            final Component info = infoDescription();
            final Style infoStyle = this.getStyleAt(info, mouseX, mouseY, infoTextX(), infoTextY(), descriptionTextWidth());
            if (infoStyle != null && infoStyle.getClickEvent() != null) {
                return true;
            }

            // fixme: do not hardcode any of this, we need a proper solution here
            final String infoLine = this.getLineAt(info, mouseX, mouseY, infoTextX(), infoTextY(), descriptionTextWidth());
            return infoLine != null && (infoLine.contains("What data is collected?")
                    || infoLine.contains("List of installed mods that use FastStats")
                    || infoLine.contains("Additional Metrics"));
        }

        // fixme: hacky bs
        private @Nullable Style getStyleAt(final Component component, final double mouseX, final double mouseY, final int textX, final int textY, final int textWidth) {
            final var line = this.getFormattedLineAt(component, mouseX, mouseY, textX, textY, textWidth);
            if (line == null) {
                return null;
            }

            final var relativeX = mouseX - textX;
            final var currentX = new double[]{0};
            final var style = line.visit((segmentStyle, text) -> {
                final float segmentWidth = this.font.getSplitter().stringWidth(FormattedText.of(text, segmentStyle));
                if (relativeX >= currentX[0] && relativeX <= currentX[0] + segmentWidth) {
                    return Optional.of(segmentStyle);
                }

                currentX[0] += segmentWidth;
                return Optional.empty();
            }, Style.EMPTY);

            return style.orElse(null);
        }

        private @Nullable String getLineAt(final Component component, final double mouseX, final double mouseY, final int textX, final int textY, final int textWidth) {
            final FormattedText line = this.getFormattedLineAt(component, mouseX, mouseY, textX, textY, textWidth);
            return line == null ? null : line.getString();
        }

        private @Nullable FormattedText getFormattedLineAt(final Component component, final double mouseX, final double mouseY, final int textX, final int textY, final int textWidth) {
            if (mouseX < textX || mouseX > textX + textWidth || mouseY < textY) {
                return null;
            }

            final List<FormattedText> lines = this.font.getSplitter().splitLines(component, textWidth, Style.EMPTY);
            final int line = (int) ((mouseY - textY) / this.font.lineHeight);
            if (line < 0 || line >= lines.size()) {
                return null;
            }

            return lines.get(line);
        }

        private int descriptionTextX() {
            final int panelWidth = panelWidth();
            final int panelX = panelX();
            final int contentInset = contentInset(panelWidth);
            return panelX + contentInset + scaled(BOX_PADDING_X);
        }

        private int descriptionTextY() {
            return descriptionY() + scaled(BOX_PADDING_Y);
        }

        private int infoTextX() {
            return descriptionTextX();
        }

        private int infoTextY() {
            return infoY() + scaled(BOX_PADDING_Y);
        }

        private int contentLeft() {
            final int panelWidth = panelWidth();
            final int panelX = panelX();
            final int contentInset = contentInset(panelWidth);
            return panelX + contentInset + 2;
        }

        private int contentWidth() {
            final int panelWidth = panelWidth();
            final int contentInset = contentInset(panelWidth);
            return panelWidth - contentInset * 2 - 4;
        }

        private int descriptionY() {
            return contentTop() - this.scrollOffset;
        }

        private int baseInfoY() {
            return contentTop() + descriptionHeight() + scaled(BOX_GAP) - this.scrollOffset;
        }

        private int infoY() {
            return this.baseInfoY();
        }

        private int scrollTop() {
            return panelY() + scaled(SCROLL_TOP_OFFSET);
        }

        private int scrollBottom() {
            return panelY() + panelHeight() - 1;
        }

        private int checkboxY() {
            return this.infoY() + infoHeight() + scaled(CHECKBOX_TOP_GAP);
        }

        private int buttonY() {
            return checkboxY() + scaled(CHECKBOX_GAP) * 2 + buttonHeight() + scaled(BUTTON_TOP_GAP);
        }

        private int contentBottom() {
            return contentTop() + descriptionHeight() + scaled(BOX_GAP) + infoHeight() + scaled(CHECKBOX_TOP_GAP)
                    + scaled(CHECKBOX_GAP) * 2 + buttonHeight() + scaled(BUTTON_TOP_GAP) + buttonHeight();
        }

        private int maxScrollOffset() {
            return Math.max(0, contentBottom() - scrollBottom());
        }

        private void updateWidgetPositions() {
            this.scrollOffset = Math.min(this.scrollOffset, this.maxScrollOffset());

            final int buttonGap = scaled(BUTTON_GAP);
            final int buttonWidth = (contentWidth() - buttonGap) / 2;
            this.submitMetricsWidget.setY(checkboxY());
            this.submitAdditionalMetricsWidget.setY(checkboxY() + scaled(CHECKBOX_GAP));
            this.errorTrackingWidget.setY(checkboxY() + scaled(CHECKBOX_GAP) * 2);
            this.submitMetricsWidget.visible = isInsideScrollArea(this.submitMetricsWidget.getY(), this.submitMetricsWidget.getHeight());
            this.submitAdditionalMetricsWidget.visible = isInsideScrollArea(this.submitAdditionalMetricsWidget.getY(), this.submitAdditionalMetricsWidget.getHeight());
            this.errorTrackingWidget.visible = isInsideScrollArea(this.errorTrackingWidget.getY(), this.errorTrackingWidget.getHeight());
            this.declineButton.setX(contentLeft());
            this.declineButton.setY(buttonY());
            this.declineButton.setWidth(buttonWidth);
            this.declineButton.setHeight(buttonHeight());
            this.acceptButton.setX(contentLeft() + buttonWidth + buttonGap);
            this.acceptButton.setY(buttonY());
            this.acceptButton.setWidth(buttonWidth);
            this.acceptButton.setHeight(buttonHeight());
            this.declineButton.visible = isInsideScrollArea(this.declineButton.getY(), this.declineButton.getHeight());
            this.acceptButton.visible = isInsideScrollArea(this.acceptButton.getY(), this.acceptButton.getHeight());
        }

        private Component additionalMetricsLabel() {
            if (this.submitMetricsSelected) {
                return Onboarding.submitAdditionalMetrics;
            }

            return Onboarding.submitAdditionalMetrics.copy().withStyle(ChatFormatting.GRAY);
        }

        private boolean isInsideScrollArea(final int y, final int height) {
            return y + height > scrollTop() && y < scrollBottom();
        }

        // fixme: there has to be some inbuilt way to do this, right? RIGHT???
        private void renderScrollbar(final GuiGraphicsExtractor graphics, final int panelX, final int panelY, final int panelWidth, final int panelHeight, final int color) {
            final int maxScrollOffset = this.maxScrollOffset();
            if (maxScrollOffset <= 0) {
                return;
            }

            final int trackX = panelX + panelWidth - 10;
            final int trackY = scrollTop();
            final int trackHeight = scrollBottom() - scrollTop();
            final int contentHeight = contentBottom() - scrollTop();
            final int thumbHeight = Math.clamp((long) trackHeight * trackHeight / contentHeight, 18, trackHeight);
            final int thumbY = trackY + this.scrollOffset * (trackHeight - thumbHeight) / maxScrollOffset;
            graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0x55FFFFFF);
            graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, color);
        }

        private int descriptionTextWidth() {
            final int panelWidth = panelWidth();
            final int contentInset = contentInset(panelWidth);
            final int descriptionWidth = panelWidth - contentInset * 2;
            return Math.max(1, descriptionWidth - scaled(BOX_PADDING_X) * 2);
        }

        private int descriptionHeight() {
            return Math.max(scaled(76), this.height / 5);
        }

        private int infoHeight() {
            return Math.max(scaled(60), this.font.wordWrapHeight(infoDescription(), descriptionTextWidth()) + scaled(BOX_PADDING_Y) * 2);
        }

        // todo: maybe an object that keeps track of that? collapsed text and everything…
        private Component infoDescription() {
            final MutableComponent component = Component.literal("")
                    .append(collapsible("What data is collected?", this.collectedDataExpanded));
            if (this.collectedDataExpanded) {
                for (final var item : alwaysCollectedData) {
                    component.append("\n· ").append(item);
                }
                component.append("\nRead more here: ")
                        .append(link(INFO_URL, INFO_URL));
            }

            component.append("\n\n")
                    .append(collapsible("List of installed mods that use FastStats", this.installedModsExpanded));
            if (this.installedModsExpanded) mods.forEach(mod -> {
                component.append("\n· ").append(mod.name());

                for (final var metric : mod.metrics()) {
                    component.append("\n  ").append(metric);
                }

                final var length = mod.additionalMetrics().length;
                if (length == 0) return;

                final boolean expanded = expandedDetails.contains(mod.name() + "-additional"); // todo: still no toggle
                final var collapsible = collapsible("Additional Metrics (" + length + ")", expanded);
                component.append("\n  ").append(collapsible);
                if (expanded) for (final var metric : mod.additionalMetrics()) {
                    component.append("\n  · ").append(metric);
                }
            });

            return component;
        }

        private int panelX() {
            return (this.width - panelWidth()) / 2;
        }

        private int panelY() {
            return scaled(PANEL_MARGIN);
        }

        private int panelWidth() {
            final int margin = scaled(PANEL_SIDE_MARGIN);
            final int availableWidth = Math.max(1, this.width - margin * 2);
            return Math.min(availableWidth, scaled(MAX_PANEL_WIDTH));
        }

        private int panelHeight() {
            return Math.max(1, this.height - panelY() * 2);
        }

        private int contentTop() {
            return panelY() + scaled(CONTENT_TOP_OFFSET);
        }

        private int buttonHeight() {
            return Math.max(BUTTON_HEIGHT, scaled(BUTTON_HEIGHT));
        }

        private int scaled(final int value) {
            return Math.max(1, Math.round(value * this.layoutScale()));
        }

        private float layoutScale() {
            if (this.width <= 0 || this.height <= 0) {
                return 1.0F;
            }

            final float widthScale = this.width / (float) REFERENCE_WIDTH;
            final float heightScale = this.height / (float) REFERENCE_HEIGHT;
            return Math.clamp(Math.min(widthScale, heightScale), 0.65F, 1.25F);
        }

        private int contentInset(final int panelWidth) {
            return Math.clamp(panelWidth / 8, scaled(24), scaled(160));
        }

        private static Component collapsible(final String text, final boolean expanded) {
            return Component.literal((expanded ? "˅ " : "˃ ") + text)
                    .withStyle(ChatFormatting.YELLOW);
        }

        private static Component link(final String text, final String url) {
            return Component.literal(text).withStyle(style -> style
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true));
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }
    }
}
