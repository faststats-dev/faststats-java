package dev.faststats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ErrorReportContextTest {
    @Test
    public void platformDataIsReportedWithoutMetrics() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var context = new MockContext.Factory()
                .defaultMetrics((ctx, data) -> data.addProperty("server_type", "Paper"))
                .errorTrackerService(tracker)
                .create();
        try {
            assertTrue(context.metrics().isEmpty());
            tracker.trackError("boom");

            final var service = (SimpleErrorTrackerService) context.errorTrackerService().orElseThrow();
            final var data = service.createData();
            assertNotNull(data);

            final var reportContext = data.getAsJsonObject("context");
            assertEquals("Paper", reportContext.get("server_type").getAsString());
            assertTrue(reportContext.has("java_version"));
            assertTrue(reportContext.has("os_name"));
        } finally {
            context.shutdown();
        }
    }
}
