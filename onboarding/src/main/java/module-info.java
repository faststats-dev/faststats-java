import dev.faststats.screen.ScreenManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding {
    exports dev.faststats.screen;
    exports dev.faststats.screen.onboarding;

    requires java.desktop;
    requires transitive dev.faststats;

    requires static org.jspecify;

    uses ScreenManager;
}
