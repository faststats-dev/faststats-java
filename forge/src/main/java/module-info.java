import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.forge {
    exports dev.faststats.forge;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats;
    requires net.minecraftforge.fmlloader;
    requires net.minecraftforge.forgespi;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
