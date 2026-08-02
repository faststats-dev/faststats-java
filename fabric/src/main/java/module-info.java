import dev.faststats.fabric.screen.FabricScreenManager;
import dev.faststats.screen.ScreenManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.fabric {
    exports dev.faststats.fabric.compat;
    exports dev.faststats.fabric.screen;
    exports dev.faststats.fabric;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats.onboarding;
    requires dev.faststats;
    requires java.desktop;
    requires java.logging;
    requires net.fabricmc.loader;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    provides ScreenManager with FabricScreenManager;

    uses dev.faststats.fabric.compat.CompatibilityLayer;
}
