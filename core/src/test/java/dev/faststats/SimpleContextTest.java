package dev.faststats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimpleContextTest {
    @Test
    public void contextWithoutAttachedServicesThrows() {
        final var error = assertThrows(IllegalStateException.class, () -> new MockContext.Factory().create());
        assertEquals("Context created without any service attached, was this intentional?", error.getMessage());
    }

    @Test
    public void contextWithAttachedServicesAndDisabledFeaturesDoesNotThrow() {
        assertDoesNotThrow(() -> new MockContext.Factory()
                .allFeaturesDisabled()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .create()
        );
    }

    @Test
    public void firstRunDoesNotAttachServices() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .firstRun()
                .create();

        assertFalse(context.metrics().isPresent());
        assertFalse(context.errorTrackerService().isPresent());
        assertFalse(context.featureFlagService().isPresent());
        assertFalse(context.performanceAnalyzer().isPresent());
    }

    @Test
    public void repeatedRunDoesAttachServices() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();

        assertTrue(context.metrics().isPresent());
        assertTrue(context.errorTrackerService().isPresent());
        assertTrue(context.featureFlagService().isPresent());
        assertTrue(context.performanceAnalyzer().isPresent());
    }
}
