package com.example.mixin;

import dev.faststats.FastStatsRegistry;
import dev.faststats.config.SimpleConfig;
import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin {
    private static volatile @Unique boolean onboardingOpened = false;

    @SuppressWarnings("IllegalDependencyOnInternalPackage")
    @Inject(method = "init", at = @At("TAIL"))
    private void openOnboardingScreen(final CallbackInfo ci) {
        final var config = (SimpleConfig) FastStatsRegistry.instance().config();
        if (!onboardingOpened && config.firstRun()) {
            onboardingOpened = true;
            OnboardingDefinition.create().open();
        }
    }
}
