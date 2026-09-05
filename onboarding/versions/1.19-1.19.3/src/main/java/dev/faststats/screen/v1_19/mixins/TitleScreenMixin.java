package dev.faststats.screen.v1_19.mixins;

import dev.faststats.screen.onboarding.FirstRunOnboarding;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$open(final CallbackInfo ci) {
        FirstRunOnboarding.openIfNeeded();
    }
}
