import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.minestom {
    exports dev.faststats.minestom;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats.core;
    requires net.minestom.server;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
