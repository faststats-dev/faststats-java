package dev.faststats.screen.minecraft.mixin;

import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.minecraft.ConsentScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    private OptionsScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$addButton(final CallbackInfo callback) {
        addRenderableWidget(new Button(width - 104, 6, 98, 20, Component.literal(ConsentText.BUTTON), button -> minecraft.setScreen(new ConsentScreen(this))));
    }
}
