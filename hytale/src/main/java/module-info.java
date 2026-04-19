import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.hytale {
    exports dev.faststats.hytale;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats.core;
    requires java.logging;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    provides dev.faststats.core.internal.LoggerFactory with dev.faststats.hytale.logger.HytaleLoggerFactory;
}
