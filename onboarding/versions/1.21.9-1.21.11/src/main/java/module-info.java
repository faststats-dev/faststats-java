import dev.faststats.screen.v1_19_4.MinecraftScreenManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding.minecraft.v1_21_9 {
    requires dev.faststats.onboarding;
    requires java.desktop;

    requires static net.fabricmc.loader;
    requires static org.jspecify;
    requires static org.spongepowered.mixin;

    provides dev.faststats.screen.ScreenManager with MinecraftScreenManager;
}
