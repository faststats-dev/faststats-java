package dev.faststats;

import java.util.Set;

/**
 * A non-sensitive description of a live FastStats consumer.
 *
 * @since 0.29.2
 */
public record FastStatsRegistration(
        String projectName,
        String sdkName,
        String sdkVersion,
        boolean metrics,
        boolean errorTracking,
        boolean featureFlags,
        Set<String> additionalMetrics
) {
    public FastStatsRegistration {
        additionalMetrics = Set.copyOf(additionalMetrics);
    }
}
