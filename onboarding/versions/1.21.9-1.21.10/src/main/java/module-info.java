import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding.minecraft.v1_21_9 {
    requires dev.faststats.onboarding;

    requires static net.fabricmc.loader;
    requires static org.jspecify;
    requires static org.spongepowered.mixin;
}
