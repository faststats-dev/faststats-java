import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.bungee {
    exports dev.faststats.bungee;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats;
    requires java.logging;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
