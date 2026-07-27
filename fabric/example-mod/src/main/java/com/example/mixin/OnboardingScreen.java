package com.example.mixin;

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

@NullMarked
@Mixin(TitleScreen.class)
public abstract class OnboardingScreen {
    @Unique
    private static boolean dismissed;

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void showFastStatsOnboardingScreen(final CallbackInfo cir) {
        if (!dismissed) {
            Minecraft.getInstance().setScreenAndShow(new Onboarding());
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
        private boolean submitAdditionalMetricsSelected = true;
        private boolean errorTrackingSelected = true;
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
                    .pos(contentLeft(), checkboxY() + CHECKBOX_GAP)
                    .onValueChange((checkbox, selected) -> this.submitAdditionalMetricsSelected = selected)
                    .selected(this.submitAdditionalMetricsSelected && this.submitMetricsSelected)
                    .build());
            this.submitAdditionalMetricsWidget.active = this.submitMetricsSelected;
            this.submitAdditionalMetricsWidget.setAlpha(this.submitMetricsSelected ? 1.0F : 0.45F);
            this.errorTrackingWidget = this.addRenderableWidget(Checkbox.builder(Onboarding.errorTracking, this.font)
                    .pos(contentLeft(), checkboxY() + CHECKBOX_GAP * 2)
                    .onValueChange((checkbox, selected) -> this.errorTrackingSelected = selected)
                    .selected(this.errorTrackingSelected)
                    .build());

            final var buttonGap = 2;
            final var buttonWidth = (contentWidth() - buttonGap) / 2;
            this.declineButton = this.addRenderableWidget(Button.builder(Component.literal("Decline All").withColor(TextColor.RED), button -> {
                // todo: disable all… close immediately or let the user also click "confirm"?
                this.onClose();
            }).bounds(contentLeft(), buttonY(), buttonWidth, 20).build());
            this.acceptButton = this.addRenderableWidget(Button.builder(Component.literal("Confirm Selection").withColor(TextColor.GREEN), button -> {
                // todo: save selection
                this.onClose();
            }).bounds(contentLeft() + buttonWidth + buttonGap, buttonY(), buttonWidth, 20).build());
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

            final int panelWidth = Math.min(this.width - 32, 1186);
            final int panelHeight = this.height - 54;
            final int panelX = (this.width - panelWidth) / 2;
            final int panelY = 27;
            final int border = 0xFFFFFFFF;
            final int text = 0xFFE0E0E0;

            graphics.outline(panelX, panelY, panelWidth, panelHeight, border);
            graphics.centeredText(this.font, title, this.width / 2, panelY + 45, text);

            final int contentInset = contentInset(panelWidth);
            final int descriptionX = panelX + contentInset;
            final int descriptionY = descriptionY();
            final int descriptionWidth = panelWidth - contentInset * 2;
            final int descriptionHeight = descriptionHeight();
            graphics.enableScissor(panelX + 1, scrollTop(), panelX + panelWidth - 1, scrollBottom());
            graphics.fill(descriptionX + 1, descriptionY + 1, descriptionX + descriptionWidth - 1, descriptionY + descriptionHeight - 1, 0xFF000000);
            graphics.outline(descriptionX, descriptionY, descriptionWidth, descriptionHeight, border);
            graphics.textWithWordWrap(this.font, description, descriptionX + 24, descriptionY + 16, descriptionWidth - 48, text, false);

            final int infoY = descriptionY + descriptionHeight + 12;
            final int infoHeight = infoHeight();
            graphics.fill(descriptionX + 1, infoY + 1, descriptionX + descriptionWidth - 1, infoY + infoHeight - 1, 0xFF000000);
            graphics.outline(descriptionX, infoY, descriptionWidth, infoHeight, border);
            graphics.textWithWordWrap(this.font, infoDescription(), descriptionX + 24, infoY + 16, descriptionWidth - 48, text, false);
            graphics.disableScissor();
            this.renderScrollbar(graphics, panelX, panelY, panelWidth, panelHeight, border);
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
            final int panelWidth = Math.min(this.width - 32, 1186);
            final int panelX = (this.width - panelWidth) / 2;
            final int contentInset = contentInset(panelWidth);
            return panelX + contentInset + 24;
        }

        private int descriptionTextY() {
            return descriptionY() + 16;
        }

        private int infoTextX() {
            return descriptionTextX();
        }

        private int infoTextY() {
            return infoY() + 16;
        }

        private int contentLeft() {
            final int panelWidth = Math.min(this.width - 32, 1186);
            final int panelX = (this.width - panelWidth) / 2;
            final int contentInset = contentInset(panelWidth);
            return panelX + contentInset + 2;
        }

        private int contentWidth() {
            final int panelWidth = Math.min(this.width - 32, 1186);
            final int contentInset = contentInset(panelWidth);
            return panelWidth - contentInset * 2 - 4;
        }

        // fixme: magic numbers here we go!!! all of this is bound to break :)
        private static final int CHECKBOX_GAP = 30;
        
        private int descriptionY() {
            return 27 + 122 - this.scrollOffset;
        }

        private int baseInfoY() {
            return 27 + 122 + descriptionHeight() + 12 - this.scrollOffset;
        }

        private int infoY() {
            return this.baseInfoY();
        }

        private int scrollTop() {
            return 27 + 88;
        }

        private int scrollBottom() {
            return this.height - 55;
        }

        private int checkboxY() {
            return this.infoY() + infoHeight() + 22;
        }
        
        private int buttonY() {
            return checkboxY() + CHECKBOX_GAP * 2 + 20 + 16;
        }

        private int contentBottom() {
            return 27 + 122 + descriptionHeight() + 12 + infoHeight() + 22 + CHECKBOX_GAP * 2 + 20 + 16 + 20;
        }

        private int maxScrollOffset() {
            return Math.max(0, contentBottom() - scrollBottom());
        }

        private void updateWidgetPositions() {
            this.scrollOffset = Math.min(this.scrollOffset, this.maxScrollOffset());

            final int buttonGap = 2;
            final int buttonWidth = (contentWidth() - buttonGap) / 2;
            this.submitMetricsWidget.setY(checkboxY());
            this.submitAdditionalMetricsWidget.setY(checkboxY() + CHECKBOX_GAP);
            this.errorTrackingWidget.setY(checkboxY() + CHECKBOX_GAP * 2);
            this.submitMetricsWidget.visible = isInsideScrollArea(this.submitMetricsWidget.getY(), this.submitMetricsWidget.getHeight());
            this.submitAdditionalMetricsWidget.visible = isInsideScrollArea(this.submitAdditionalMetricsWidget.getY(), this.submitAdditionalMetricsWidget.getHeight());
            this.errorTrackingWidget.visible = isInsideScrollArea(this.errorTrackingWidget.getY(), this.errorTrackingWidget.getHeight());
            this.declineButton.setX(contentLeft());
            this.declineButton.setY(buttonY());
            this.declineButton.setWidth(buttonWidth);
            this.declineButton.setHeight(20);
            this.acceptButton.setX(contentLeft() + buttonWidth + buttonGap);
            this.acceptButton.setY(buttonY());
            this.acceptButton.setWidth(buttonWidth);
            this.acceptButton.setHeight(20);
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
            final int panelWidth = Math.min(this.width - 32, 1186);
            final int contentInset = contentInset(panelWidth);
            final int descriptionWidth = panelWidth - contentInset * 2;
            return descriptionWidth - 48;
        }

        private int descriptionHeight() {
            return Math.max(76, this.height / 5);
        }

        private int infoHeight() {
            return Math.max(60, this.font.wordWrapHeight(infoDescription(), descriptionTextWidth()) + 32);
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

        private static int contentInset(final int panelWidth) {
            return Math.clamp(panelWidth / 8, 24, 160);
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
