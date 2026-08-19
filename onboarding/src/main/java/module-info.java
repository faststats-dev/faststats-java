import dev.faststats.screen.ScreenManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding {
    exports dev.faststats.screen.onboarding;
    exports dev.faststats.screen.registry;
    exports dev.faststats.screen;

    requires dev.faststats.config;
    requires java.desktop;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    requires transitive dev.faststats;

    uses ScreenManager;
}
