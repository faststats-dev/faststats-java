package dev.faststats.screen.registry;

import java.util.Set;

public record FastStatsRegistration(
        String projectName,
        String sdkName,
        String sdkVersion,
        boolean metrics,
        boolean errorTracking,
        boolean featureFlags,
        Set<String> additionalMetrics
) {
}
