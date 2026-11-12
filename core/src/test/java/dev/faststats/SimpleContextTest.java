package dev.faststats;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void firstRunAttachesServicesWithoutSubmitting() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .firstRun()
                .create();

        assertTrue(context.metrics().isPresent());
        assertTrue(context.errorTrackerService().isPresent());
        assertTrue(context.featureFlagService().isPresent());
        assertFalse(context.isSubmitting());
    }

    @Test
    public void featureFlagsResolveToDefaultWhileNotSubmitting() throws Exception {
        final var context = new MockContext.Factory()
                .featureFlagService(FeatureFlagService.Factory::create)
                .firstRun()
                .create();
        final var flag = context.featureFlagService().orElseThrow().define("test", true);

        assertTrue(flag.fetch().get(1, TimeUnit.SECONDS));
        assertFalse(flag.getCached().isPresent());
        assertThrows(ExecutionException.class, () -> flag.optIn().get(1, TimeUnit.SECONDS));
    }

    @Test
    public void submissionCanBeStartedAndStopped() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .firstRun()
                .create();

        context.startSubmission();
        assertTrue(context.isSubmitting());
        context.stopSubmission();
        assertFalse(context.isSubmitting());
        context.shutdown();
    }

    @Test
    public void disabledContextNeverSubmits() {
        final var context = new MockContext.Factory()
                .noToken()
                .metrics(Metrics.Factory::create)
                .create();

        context.startSubmission();
        assertFalse(context.isSubmitting());
    }

    @Test
    public void repeatedRunDoesAttachServices() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .create();

        assertTrue(context.metrics().isPresent());
        assertTrue(context.errorTrackerService().isPresent());
        assertTrue(context.featureFlagService().isPresent());
        assertTrue(context.isSubmitting());
    }

    @Test
    public void updateConfigReplacesConfigButKeepsServerId() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .create();
        final var serverId = context.getConfig().serverId();

        context.updateConfig(config(serverId, false));
        assertFalse(context.getConfig().submitMetrics());

        assertThrows(IllegalArgumentException.class, () -> context.updateConfig(config(UUID.randomUUID(), true)));
        assertEquals(serverId, context.getConfig().serverId());
        context.shutdown();
    }

    @Test
    public void failedReadyHookKeepsContextNotReady() {
        final var calls = new AtomicInteger();
        final var context = new SimpleContext.Builder("test")
                .loggerFactory(new dev.faststats.internal.PlatformLoggerFactory((level, throwable, message) -> {
                }))
                .config(config(UUID.randomUUID(), true))
                .projectName("Mock")
                .token("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .metrics(Metrics.Factory::create)
                .onReady(ignored -> {
                    if (calls.incrementAndGet() == 1) throw new IllegalStateException("too early");
                })
                .build();

        context.ready();
        assertEquals(1, calls.get());
        context.ready();
        assertEquals(2, calls.get());
        context.ready();
        assertEquals(2, calls.get());
        context.shutdown();
    }

    @Test
    public void failedSchedulingKeepsSubmissionInactive() {
        final var canceled = new AtomicInteger();
        final var scheduled = new AtomicInteger();
        final var context = new SimpleContext.Builder("test")
                .loggerFactory(new dev.faststats.internal.PlatformLoggerFactory((level, throwable, message) -> {
                }))
                .config(config(UUID.randomUUID(), true))
                .projectName("Mock")
                .token("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .errorTrackerService(ErrorTracker.contextUnaware())
                .metrics(Metrics.Factory::create)
                .scheduler((task, initialDelay, period, unit) -> {
                    // the error task is scheduled first, the metrics task is rejected
                    if (scheduled.incrementAndGet() == 2) throw new IllegalStateException("rejected");
                    return canceled::incrementAndGet;
                })
                .build();

        assertThrows(IllegalStateException.class, context::startSubmission);
        assertFalse(context.isSubmitting());
        assertEquals(1, canceled.get());
        context.shutdown();
    }

    @Test
    public void buildHookReceivesConsentBuilder() {
        final var required = new java.util.concurrent.atomic.AtomicBoolean();
        final FastStatsContext.ConsentBuilder builder = new SimpleContext.ConsentBuilder("test")
                .loggerFactory(new dev.faststats.internal.PlatformLoggerFactory((level, throwable, message) -> {
                }))
                .config(config(UUID.randomUUID(), true))
                .projectName("Mock")
                .onBuild((context, self) -> required.set(((SimpleContext.ConsentBuilder) self).isConsentRequired()));

        // consent can be required before and after configuring services
        final var context = builder.metrics(Metrics.Factory::create)
                .requireConsent()
                .token("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .build();

        assertTrue(builder.isConsentRequired());
        assertTrue(required.get());
        context.shutdown();
    }

    private static Config config(final UUID serverId, final boolean enabled) {
        return new Config() {
            @Override
            public UUID serverId() {
                return serverId;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public boolean submitMetrics() {
                return enabled;
            }

            @Override
            public boolean errorTracking() {
                return enabled;
            }

            @Override
            public boolean additionalMetrics() {
                return enabled;
            }

            @Override
            public boolean debug() {
                return false;
            }
        };
    }
}
