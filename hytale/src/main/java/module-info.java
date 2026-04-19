import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.hytale {
    exports dev.faststats.hytale;

    requires com.google.gson;
    requires dev.faststats.core;
    requires java.logging;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}