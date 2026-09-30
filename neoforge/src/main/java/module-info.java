import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.neoforge {
    exports dev.faststats.neoforge.compat;
    exports dev.faststats.neoforge;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats.onboarding;
    requires dev.faststats;
    requires fml_loader;
    requires java.net.http;
    requires net.neoforged.bus;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    uses dev.faststats.neoforge.compat.CompatibilityLayer;
}
