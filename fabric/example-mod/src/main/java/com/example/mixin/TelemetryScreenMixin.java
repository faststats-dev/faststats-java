package com.example.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TelemetryInfoScreen.class)
public final class TelemetryScreenMixin {
    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;visitWidgets(Ljava/util/function/Consumer;)V"
            )
    )
    private void addFastStatsButton(
            final CallbackInfo ci,
            @Local(name = "upperContentButtons") final LinearLayout upperContentButtons
    ) {
        upperContentButtons.addChild(Button.builder(Component.literal("FastStats"), _ -> {
            OnboardingDefinition.create().onClose(() -> {
                Minecraft.getInstance().setScreenAndShow((Screen) (Object) this);
            }).open();
        }).build());
    }
}
