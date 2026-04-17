package dev.faststats.example;

import dev.faststats.core.Settings;

import java.net.URI;

public final class SettingsExample {
    // Recommended: create settings with just a token
    public static final Settings SETTINGS = Settings.withToken("YOUR_TOKEN_HERE");

    // Or use the factory for full control
    public static final Settings ALL_SETTINGS = Settings.factory()
            .url(URI.create("https://metrics.example.com/v1/collect")) // only for different metrics servers (mainly for testing)
            .debug(true) // Enable debug mode for development and testing
            .token("YOUR_TOKEN_HERE") // required -> token can be found in the settings of your project
            .create();
}
