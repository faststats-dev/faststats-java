import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.fabric {
    exports dev.faststats.fabric;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats;
    requires net.fabricmc.loader;
    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
