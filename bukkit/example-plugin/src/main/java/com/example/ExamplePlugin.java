package com.example;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.data.Metric;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.atomic.AtomicInteger;

public class ExamplePlugin extends JavaPlugin {
    // context-aware error tracker, automatically tracks errors in the same class loader
    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            // Ignore specific errors and messages
            .ignoreError(InvocationTargetException.class, "Expected .* but got .*") // Ignored an error with a message
            .ignoreError(AccessDeniedException.class); // Ignored a specific error type

    // context-unaware error tracker, does not automatically track errors
    public static final ErrorTracker CONTEXT_UNAWARE_ERROR_TRACKER = ErrorTracker.contextUnaware()
            // Anonymize error messages if required
            .anonymize("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "[email hidden]") // Email addresses
            .anonymize("Bearer [A-Za-z0-9._~+/=-]+", "Bearer [token hidden]") // Bearer tokens in error messages
            .anonymize("AKIA[0-9A-Z]{16}", "[aws-key hidden]") // AWS access key IDs
            .anonymize("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[uuid hidden]") // UUIDs (e.g. session/user IDs)
            .anonymize("([?&](?:api_?key|token|secret)=)[^&\\s]+", "$1[redacted]"); // API keys in query strings

    private final AtomicInteger gameCount = new AtomicInteger();

    private final BukkitMetrics metrics = BukkitMetrics.factory()
            .url(URI.create("https://metrics.example.com/v1/collect")) // For self-hosted metrics servers only

            // Custom example metrics
            // For this to work you have to create a corresponding data source in your project settings first
            .addMetric(Metric.number("example_metric", () -> 42))
            .addMetric(Metric.number("game_count", gameCount::get))
            .addMetric(Metric.string("example_string", () -> "Hello, World!"))
            .addMetric(Metric.bool("example_boolean", () -> true))
            .addMetric(Metric.stringArray("example_string_array", () -> new String[]{"Option 1", "Option 2"}))
            .addMetric(Metric.numberArray("example_number_array", () -> new Number[]{1, 2, 3}))
            .addMetric(Metric.booleanArray("example_boolean_array", () -> new Boolean[]{true, false}))

            // Attach an error tracker
            // This must be enabled in the project settings
            .errorTracker(ERROR_TRACKER)

            .onFlush(() -> gameCount.set(0)) // Reset game count on flush

            .debug(true) // Enable debug mode for development and testing

            .token("YOUR_TOKEN_HERE") // required -> token can be found in the settings of your project
            .create(this);

    @Override
    public void onEnable() {
        metrics.ready(); // register additional error handlers
    }

    @Override
    public void onDisable() {
        metrics.shutdown(); // safely shut down metrics submission
    }

    public void doSomethingWrong() {
        try {
            // Do something that might throw an error
            throw new RuntimeException("Something went wrong!");
        } catch (final Exception e) {
            CONTEXT_UNAWARE_ERROR_TRACKER.trackError(e);
        }
    }

    public void startGame() {
        gameCount.incrementAndGet();
    }
}
