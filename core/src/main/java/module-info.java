import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats {
    exports dev.faststats.data;
    exports dev.faststats.internal;
    exports dev.faststats;

    requires com.google.gson;
    requires java.logging;
    requires java.net.http;
    requires java.sql;
    requires jdk.jfr;

    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    uses dev.faststats.SdkInfo.UserAgentProvider;
    uses dev.faststats.internal.LoggerFactory;
}
