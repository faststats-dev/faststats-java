import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.core {
    exports dev.faststats.core.data;
    exports dev.faststats.core.flags;
    exports dev.faststats.core.internal;
    exports dev.faststats.core;

    requires com.google.gson;
    requires java.logging;
    requires java.net.http;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    uses dev.faststats.core.internal.LoggerFactory;
}
