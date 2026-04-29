package dev.faststats.example;

import dev.faststats.ErrorTracker;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.AccessDeniedException;

public final class ErrorTrackerExample {
    // Context-aware: automatically tracks uncaught errors from the same class loader
    public static final ErrorTracker CONTEXT_AWARE = ErrorTracker.contextAware()
            .ignoreError(InvocationTargetException.class, "Expected .* but got .*")
            .ignoreError(AccessDeniedException.class);

    // Context-unaware: only tracks errors passed to trackError() manually
    public static final ErrorTracker CONTEXT_UNAWARE = ErrorTracker.contextUnaware()
            .anonymize("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "[email hidden]")
            .anonymize("Bearer [A-Za-z0-9._~+/=-]+", "Bearer [token hidden]")
            .anonymize("AKIA[0-9A-Z]{16}", "[aws-key hidden]")
            .anonymize("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[uuid hidden]")
            .anonymize("([?&](?:api_?key|token|secret)=)[^&\\s]+", "$1[redacted]");

    public static void manualTracking() {
        try {
            throw new RuntimeException("Something went wrong!");
        } catch (final Exception e) {
            CONTEXT_UNAWARE.trackError(e);
        }
    }
}
