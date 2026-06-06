package dev.faststats.example;

import dev.faststats.FastStatsContext;
import dev.faststats.SimpleContext;
import dev.faststats.JfrPerformanceOptions;
import dev.faststats.PerformanceAnalyzer;
import dev.faststats.SqlPerformanceOptions;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;

public final class PerformanceAnalyzerExample {
    public static final FastStatsContext CONTEXT = getContextFactory()
            .performanceAnalyzer(factory -> factory
                    .sql(new SqlPerformanceOptions(Duration.ofMillis(150), 128, true))
                    .jfr(new JfrPerformanceOptions(
                            Duration.ofSeconds(5),
                            Set.of("jdk.ExecutionSample", "jdk.JavaMonitorEnter", "jdk.SocketRead", "jdk.SocketWrite"),
                            5_000,
                            PerformanceAnalyzerExample.class.getClassLoader()
                    ))
                    .create())
            .create();

    public static final PerformanceAnalyzer PERFORMANCE = CONTEXT.performanceAnalyzer().orElseThrow();

    public static void runInstrumentedQuery(final DataSource dataSource) throws SQLException {
        final var instrumented = PERFORMANCE.wrap(dataSource);

        try (final var connection = instrumented.getConnection();
             final var statement = connection.prepareStatement("UPDATE users SET last_seen = ? WHERE id = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setLong(2, 42L);
            statement.executeUpdate();
        }
    }

    public static void captureRuntimeSnapshot() {
        try (final var recorder = PERFORMANCE.recordJfrSnapshot()) {
            recorder.recording().setName("faststats-example");
            runCodeToProfile();
        } catch (final Throwable t) {
            PERFORMANCE.flush();
            throw t;
        }
    }

    public static String timeReturningMethod() throws Exception {
        return PERFORMANCE.record("load_profile", () -> loadProfile(42L));
    }

    public static void timeRunnable() {
        PERFORMANCE.record("refresh_cache", PerformanceAnalyzerExample::refreshCache);
    }

    public static void timeManualBlock() {
        try (final var recording = PERFORMANCE.startRecording("manual_block")) {
            try {
                runCodeToProfile();
            } catch (final Throwable t) {
                recording.fail();
                throw t;
            }
        }
    }

    private static void runCodeToProfile() {
    }

    private static String loadProfile(final long userId) {
        return "user-" + userId;
    }

    private static void refreshCache() {
    }

    private static SimpleContext.Factory<?, ?> getContextFactory() {
        return null;
    }
}
