package dev.faststats.fabric.screen;

import dev.faststats.screen.Button;
import dev.faststats.screen.Checkbox;
import dev.faststats.screen.Screen;
import dev.faststats.screen.ScreenManager;
import dev.faststats.screen.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class FabricScreenManager implements ScreenManager {
    @Override
    public Text newText() {
        return new FabricText();
    }

    @Override
    public void closeScreen(Screen screen) {
        Minecraft.getInstance().setScreenAndShow(new TitleScreen());
    }

    @Override
    public void openScreen(Screen screen) {
        Minecraft.getInstance().setScreenAndShow(wrap(screen));
    }

    private Component wrap(Text text) {
        return ((FabricText) text).text;
    }

    private net.minecraft.client.gui.screens.Screen wrap(Screen screen) {
        return new net.minecraft.client.gui.screens.Screen(wrap(screen.title())) {
            @Override
            protected void init() {
                super.init();
            }

        };
    }

    private net.minecraft.client.gui.components.Checkbox wrap(Checkbox checkbox, net.minecraft.client.gui.screens.Screen nmc, Screen screen) {
        var build = net.minecraft.client.gui.components.Checkbox.builder(wrap(checkbox.label()), nmc.getFont())
                .onValueChange((self, value) -> {
                    checkbox.selected(value);
                    checkbox.onStateChange(screen);
                })
                .selected(checkbox.selected())
                .build();
        var action = (Consumer<Checkbox>) self -> {
            // todo: update built checkbox selection state
            build.active = self.enabled();
            build.setAlpha(self.enabled() ? 1.0F : 0.45F);
        };
        checkbox.onStateChange((ignored, self) -> action.accept(self));
        action.accept(checkbox);
        return build;
    }

    private net.minecraft.client.gui.components.Button wrap(Button button, Screen screen) {
        return net.minecraft.client.gui.components.Button.builder(wrap(button.label()), button1 -> {
            button.onClick(screen);
        }).bounds(0, 0, button.width(), button.height()).build(); // todo: calculate x and y based on the visible content
    }
}
