package dev.faststats;

/**
 * Immutable user-controlled submission settings.
 *
 * @param submitMetrics whether default metrics may be submitted
 * @param additionalMetrics whether developer-provided metrics may be submitted
 * @param errorTracking whether errors may be submitted
 * @since 0.29.2
 */
public record SubmissionSettings(
        boolean submitMetrics,
        boolean additionalMetrics,
        boolean errorTracking
) {
    public SubmissionSettings {
        if (!submitMetrics) additionalMetrics = false;
    }
}
