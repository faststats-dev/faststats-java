package dev.faststats.screen.mixin.v26_1;

import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.v26_1.ConsentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TelemetryInfoScreen.class)
public final class TelemetryInfoScreenMixin extends Screen {
    @Unique
    private static final int faststats$SPACING = 8;

    @Unique
    private @Nullable Button faststats$button;

    private TelemetryInfoScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$addButton(final CallbackInfo callback) {
        final Component message = Component.literal(ConsentText.BUTTON);
        faststats$button = addRenderableWidget(Button.builder(message, ignored -> {
            Minecraft.getInstance().setScreenAndShow(new ConsentScreen(this));
        }).bounds(width - 156, 6, 150, Button.DEFAULT_HEIGHT).build());
        faststats$positionButton();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"), require = 0)
    private void faststats$repositionButton(final CallbackInfo callback) {
        faststats$positionButton();
    }

    @Unique
    private void faststats$positionButton() {
        final Button button = faststats$button;
        final Button privacy = faststats$findButton("telemetry_info.button.privacy_statement");
        final Button feedback = faststats$findButton("telemetry_info.button.give_feedback");
        if (button == null || privacy == null || feedback == null || privacy.getY() != feedback.getY()) return;

        final int rowWidth = privacy.getWidth() + feedback.getWidth() + button.getWidth() + faststats$SPACING * 2;
        privacy.setX((width - rowWidth) / 2);
        feedback.setX(privacy.getX() + privacy.getWidth() + faststats$SPACING);
        button.setX(feedback.getX() + feedback.getWidth() + faststats$SPACING);
        button.setY(feedback.getY());
    }

    @Unique
    private @Nullable Button faststats$findButton(final String key) {
        for (final GuiEventListener child : children()) {
            if (child instanceof final Button button
                    && button.getMessage().getContents() instanceof final TranslatableContents contents
                    && contents.getKey().equals(key)) return button;
        }
        return null;
    }
}
