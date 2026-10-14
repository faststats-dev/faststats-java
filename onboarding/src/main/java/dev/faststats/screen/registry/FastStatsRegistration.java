package dev.faststats.screen.registry;

import java.util.Set;

public record FastStatsRegistration(
        String projectName,
        boolean metrics,
        boolean errorTracking,
        Set<String> additionalMetrics
) {
}
