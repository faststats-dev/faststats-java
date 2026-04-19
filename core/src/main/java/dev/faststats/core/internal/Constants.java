package dev.faststats.core.internal;

import dev.faststats.core.SimpleMetrics;

import java.io.IOException;
import java.util.Properties;

public final class Constants {
    public static final String SDK_NAME;
    public static final String SDK_VERSION;
    public static final String BUILD_ID;

    static {
        final var properties = new Properties();
        try (final var stream = SimpleMetrics.class.getClassLoader().getResourceAsStream("/META-INF/faststats.properties")) {
            if (stream != null) properties.load(stream);
        } catch (final IOException ignored) {
        }
        SDK_NAME = properties.getProperty("name", "unknown");
        SDK_VERSION = properties.getProperty("version", "unknown");
        BUILD_ID = properties.getProperty("build-id", "unknown");
    }
}
