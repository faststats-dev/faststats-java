import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.forge {
    exports dev.faststats.forge.compat;
    exports dev.faststats.forge;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats.onboarding;
    requires dev.faststats;
    requires java.net.http;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    uses dev.faststats.forge.compat.CompatibilityLayer;
}
