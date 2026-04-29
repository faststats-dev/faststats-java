package dev.faststats;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class MetricsTest {
    @Test
    public void testCreateData() {
        final var context = new MockContext(UUID.randomUUID(), "24f9fc423ed06194065a42d00995c600", true);
        final var metrics = (SimpleMetrics) context.metrics().create();
        assumeTrue(metrics.submit(), "For this test to run, the server must be running");
    }
}
