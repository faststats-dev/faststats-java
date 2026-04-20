import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.config {
    exports dev.faststats.config;

    requires dev.faststats.core;
    requires java.logging;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
