package dev.faststats.screen.v1_16;

import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.Minecraft;
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
public final class OptionsScreenMixin extends Screen {
    private OptionsScreenMixin(final Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void faststats$addButton(final CallbackInfo callback) {
        addRenderableWidget(new Button(width - 104, 6, 98, 20, new TextComponent("FastStats"), ignored -> {
            OnboardingDefinition.create().onClose(() -> Minecraft.getInstance().setScreen(this)).open();
        }));
    }
}
