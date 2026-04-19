package dev.faststats.example;

import dev.faststats.core.flags.Attributes;
import dev.faststats.core.flags.FeatureFlag;
import dev.faststats.core.flags.FeatureFlagService;

import java.time.Duration;
import java.time.Instant;

public final class FeatureFlagExample {
    public static final FeatureFlagService SERVICE = FeatureFlagService.create(
            "YOUR_TOKEN_HERE", // token can be found in the settings of your project
            Attributes.create() // Define global attributes
                    .put("version", "1.2.3")
                    .put("java_version", System.getProperty("java.version"))
                    .put("java_vendor", System.getProperty("java.vendor")),
            Duration.ofMinutes(10) // Custom cache TTL for resolved flag values
    );

    // Define flags with default values
    public static final FeatureFlag<Boolean> NEW_COMMANDS = SERVICE.define("new_commands", false);
    public static final FeatureFlag<String> COMPRESSION = SERVICE.define("compression", "zstd");

    public static void usage() {
        // Async: waits for the server value to be fetched
        NEW_COMMANDS.whenReady().thenAccept(enabled -> {
            if (enabled) {
                // register new commands
            }
        });

        // Non-blocking: returns the cached value if present without triggering a fetch
        COMPRESSION.getCached().ifPresent(compression -> {
            switch (compression) {
                case "zstd":
                    // default compression
                    break;
                case "lz4":
                    // experimental compression
                    break;
                default:
                    break;
            }
        });

        // Refresh stale values explicitly when your code decides it is needed
        if (COMPRESSION.getExpiration().filter(Instant.now()::isAfter).isPresent()) {
            COMPRESSION.fetch();
        }

        // Opt-in/out (requires allow_specific_opt_in on server)
        NEW_COMMANDS.optIn().thenAccept(updatedValue -> {
            if (updatedValue) {
                // react to the updated server value
            }
        });
        NEW_COMMANDS.optOut().thenAccept(updatedValue -> {
            if (!updatedValue) {
                // react to the updated server value
            }
        });
    }
}
