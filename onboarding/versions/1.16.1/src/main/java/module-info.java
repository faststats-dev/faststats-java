import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding.minecraft.v1_16_1 {
    requires dev.faststats.onboarding;

    requires static net.fabricmc.loader;
    requires static org.jspecify;
    requires static org.lwjgl.opengl;
    requires static org.spongepowered.mixin;
}
