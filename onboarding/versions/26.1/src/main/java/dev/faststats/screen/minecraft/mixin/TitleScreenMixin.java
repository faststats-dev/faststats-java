package dev.faststats.screen.minecraft.mixin;

import dev.faststats.screen.onboarding.FirstRunOnboarding;
import dev.faststats.screen.minecraft.ConsentScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private TitleScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$openOnboarding(final CallbackInfo callback) {
        if (FirstRunOnboarding.shouldOpen()) minecraft.setScreenAndShow(new ConsentScreen(this));
    }
}
