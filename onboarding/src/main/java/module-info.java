import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.onboarding {
    exports dev.faststats.screen.onboarding;
    exports dev.faststats.screen.registry;

    requires dev.faststats.config;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    requires transitive dev.faststats;
}
