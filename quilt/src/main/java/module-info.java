import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.quilt {
    exports dev.faststats.quilt;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats;
    requires org.quiltmc.loader;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}