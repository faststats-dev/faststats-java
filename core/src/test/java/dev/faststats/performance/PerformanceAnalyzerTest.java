package dev.faststats.performance;

import dev.faststats.JfrPerformanceOptions;
import dev.faststats.Metrics;
import dev.faststats.MockContext;
import dev.faststats.PerformanceAnalyzer;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PerformanceAnalyzerTest {
    @Test
    public void analyzerIsOptIn() {
        final var context = new MockContext.Factory()
                .metrics(Metrics.Factory::create)
                .create();

        assertFalse(context.performanceAnalyzer().isPresent());

        context.shutdown();
    }

    @Test
    public void analyzerCanBeConfiguredWithoutMetrics() {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();

        assertTrue(context.performanceAnalyzer().isPresent());

        context.shutdown();
    }

    @Test
    public void jfrRecorderCanBeStoppedByCaller() {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(factory -> factory
                        .jfr(JfrPerformanceOptions.factory()
                                .duration(Duration.ofSeconds(1))
                                .eventNames(Set.of("jdk.ExecutionSample"))
                                .maxEvents(100)
                                .create())
                        .create())
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();
        final var recorder = analyzer.recordJfrSnapshot();
        recorder.recording().setName("faststats-test");
        recorder.close();
        assertTrue(recorder.isClosed());
        context.shutdown();
    }

    @Test
    public void recordCallableReturnsValue() throws Exception {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();

        assertEquals("value", analyzer.record("callable", () -> "value"));

        context.shutdown();
    }

    @Test
    public void recordRunnablePropagatesFailure() {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();
        final var failure = new IllegalStateException("boom");

        final var thrown = assertThrows(IllegalStateException.class, () -> analyzer.record("runnable", () -> {
            throw failure;
        }));

        assertEquals(failure, thrown);

        context.shutdown();
    }

    @Test
    public void manualTimeRecordingCanBeStoppedByCaller() {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();
        final var recording = analyzer.startRecording("manual");

        recording.close();
        recording.fail();

        assertTrue(recording.isClosed());

        context.shutdown();
    }

    @Test
    public void wrappedDataSourcePreservesJdbcReturnValues() throws Exception {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();
        final var statementCalls = new AtomicInteger();

        final var statement = proxy(Statement.class, (proxy, method, args) -> {
            if ("executeUpdate".equals(method.getName())) {
                statementCalls.incrementAndGet();
                return 3;
            }
            return defaultValue(method);
        });
        final var connection = proxy(Connection.class, (proxy, method, args) -> {
            if ("createStatement".equals(method.getName())) return statement;
            return defaultValue(method);
        });
        final var dataSource = proxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return connection;
            return defaultValue(method);
        });

        final var wrapped = analyzer.wrap(dataSource);

        assertEquals(3, wrapped.getConnection().createStatement().executeUpdate("UPDATE users SET age = 43 WHERE id = 1"));
        assertEquals(1, statementCalls.get());

        context.shutdown();
    }

    @Test
    public void wrappedStatementPreservesJdbcExceptions() {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();
        final var failure = new SQLException("boom");

        final var statement = proxy(Statement.class, (proxy, method, args) -> {
            if ("executeQuery".equals(method.getName())) throw failure;
            return defaultValue(method);
        });
        final var connection = proxy(Connection.class, (proxy, method, args) -> {
            if ("createStatement".equals(method.getName())) return statement;
            return defaultValue(method);
        });

        final var wrapped = analyzer.wrap(connection);
        final var thrown = assertThrows(SQLException.class, () ->
                wrapped.createStatement().executeQuery("SELECT * FROM users WHERE name = 'secret'")
        );

        assertEquals(failure, thrown);

        context.shutdown();
    }

    private static <T> T proxy(final Class<T> type, final InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(final Method method) {
        final var type = method.getReturnType();
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }
}
