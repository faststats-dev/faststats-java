package dev.faststats;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class PerformanceSqlAnonymizationTest {
    @Test
    public void sqlAnonymizationUsesSqlSpecificPatterns() throws Exception {
        final var context = new MockContext.Factory()
                .performanceAnalyzer(factory -> factory
                        .sql(SqlPerformanceOptions.factory()
                                .anonymize("tenant_[a-z]+", "tenant_[hidden]")
                                .create())
                        .create())
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();

        execute(analyzer, "SELECT * FROM tenant_secret WHERE name = 'alice' AND age = 43");

        assertEquals(
                "SELECT * FROM tenant_[hidden] WHERE name = ? AND age = ?",
                queryShape(analyzer)
        );

        context.shutdown();
    }

    @Test
    public void sqlAnonymizationDoesNotUseErrorTrackerPatterns() throws Exception {
        final var errorTracker = ErrorTracker.contextUnaware()
                .anonymize("tenant_[a-z]+", "tenant_[hidden]");
        final var context = new MockContext.Factory()
                .errorTrackerService(errorTracker)
                .performanceAnalyzer(PerformanceAnalyzer.Factory::create)
                .create();
        final var analyzer = context.performanceAnalyzer().orElseThrow();

        execute(analyzer, "SELECT * FROM tenant_secret WHERE name = 'alice'");

        assertEquals(
                "SELECT * FROM tenant_secret WHERE name = ?",
                queryShape(analyzer)
        );

        context.shutdown();
    }

    private static void execute(final PerformanceAnalyzer analyzer, final String sql) throws Exception {
        final var statement = proxy(Statement.class, (proxy, method, args) -> defaultValue(method));
        final var connection = proxy(Connection.class, (proxy, method, args) -> {
            if ("createStatement".equals(method.getName())) return statement;
            return defaultValue(method);
        });

        analyzer.wrap(connection).createStatement().executeQuery(sql);
    }

    private static String queryShape(final PerformanceAnalyzer analyzer) throws Exception {
        final var method = analyzer.getClass().getDeclaredMethod("createData");
        method.setAccessible(true);
        final var data = (JsonObject) method.invoke(analyzer);
        return data.getAsJsonObject("data")
                .getAsJsonArray("sql")
                .get(0)
                .getAsJsonObject()
                .get("query_shape")
                .getAsString();
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
