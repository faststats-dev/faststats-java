import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.neoforge {
    exports dev.faststats.neoforge;

    requires com.google.gson;
    requires dev.faststats.config;
    requires dev.faststats;
    requires fml_loader;
    requires net.neoforged.bus;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}