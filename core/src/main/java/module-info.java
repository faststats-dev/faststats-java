import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats {
    exports dev.faststats.data;
    exports dev.faststats.internal;
    exports dev.faststats;

    requires com.google.gson;
    requires java.logging;
    requires java.net.http;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    uses dev.faststats.internal.LoggerFactory;
}
