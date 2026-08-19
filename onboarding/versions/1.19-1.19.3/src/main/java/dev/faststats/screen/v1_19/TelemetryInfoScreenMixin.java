package dev.faststats.screen.v1_19;

import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TelemetryInfoScreen.class)
public final class TelemetryInfoScreenMixin extends Screen {
    private TelemetryInfoScreenMixin(final Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$addButton(final CallbackInfo callback) {
        addRenderableWidget(Button.builder(Component.literal("FastStats"), ignored -> {
            OnboardingDefinition.create().onClose(() -> Minecraft.getInstance().setScreen(this)).open();
        }).bounds(width - 104, 6, 98, 20).build());
    }
}
