import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.sponge {
    exports dev.faststats.sponge;

    requires com.google.gson;
    requires com.google.guice;
    requires dev.faststats.config;
    requires dev.faststats.core;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
