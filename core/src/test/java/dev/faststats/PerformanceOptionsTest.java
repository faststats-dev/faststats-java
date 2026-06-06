package dev.faststats;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class PerformanceOptionsTest {
    @Test
    public void sqlOptionsToBuilderCopiesExistingValues() {
        final var options = SqlPerformanceOptions.factory()
                .slowQueryThreshold(Duration.ofMillis(250))
                .maxQueryShapes(42)
                .includeUpdateCounts(false)
                .anonymize("tenant_[a-z]+", "tenant_[hidden]")
                .create();

        final var copy = options.toFactory()
                .maxQueryShapes(64)
                .create();

        assertEquals(Duration.ofMillis(250), copy.slowQueryThreshold());
        assertEquals(64, copy.maxQueryShapes());
        assertEquals(false, copy.includeUpdateCounts());
        assertEquals(1, copy.anonymizationEntries().size());
        assertEquals("tenant_[hidden]", copy.anonymizationEntries().get(0).getValue());
    }

    @Test
    public void jfrOptionsToBuilderCopiesExistingValues() {
        final var loader = new ClassLoader() {
        };
        final var options = JfrPerformanceOptions.factory()
                .duration(Duration.ofSeconds(2))
                .eventNames(Set.of("jdk.ExecutionSample"))
                .maxEvents(50)
                .attributionLoader(loader)
                .create();

        final var copy = options.toFactory()
                .maxEvents(75)
                .create();

        assertEquals(Duration.ofSeconds(2), copy.duration());
        assertEquals(Set.of("jdk.ExecutionSample"), copy.eventNames());
        assertEquals(75, copy.maxEvents());
        assertSame(loader, copy.attributionLoader());
        assertNull(copy.toFactory().attributionLoader(null).create().attributionLoader());
    }
}
