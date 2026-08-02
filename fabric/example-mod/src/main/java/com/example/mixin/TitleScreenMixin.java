package com.example.mixin;

import dev.faststats.screen.onboarding.OnboardingDefinition;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin extends Screen {
    @Unique
    private static final Identifier logo = Identifier.fromNamespaceAndPath("faststats", "logo");

    private TitleScreenMixin(final Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    private void addFStatsButton(final CallbackInfo ci) {
        addRenderableWidget(new ImageButton(
                this.width / 2 + 104,
                this.height / 4 + 48,
                20, 20,
                new WidgetSprites(logo, logo),
                _ -> OnboardingDefinition.create().open(),
                Component.literal("FastStats")
        ));
    }
}
