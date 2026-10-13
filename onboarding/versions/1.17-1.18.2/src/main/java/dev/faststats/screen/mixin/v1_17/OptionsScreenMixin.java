package dev.faststats.screen.mixin.v1_17;

import dev.faststats.screen.onboarding.ConsentText;
import dev.faststats.screen.v1_17.ConsentScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
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
        addRenderableWidget(new Button(width - 104, 6, 98, 20, new TextComponent(ConsentText.BUTTON), button -> minecraft.setScreen(new ConsentScreen(this))));
    }
}
