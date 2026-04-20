import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.sponge {
    exports dev.faststats.sponge;

    requires com.google.gson;
    requires com.google.guice;
    requires dev.faststats;
    requires java.logging;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;
}
