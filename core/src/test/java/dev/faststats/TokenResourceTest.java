package dev.faststats;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static dev.faststats.SimpleContext.Factory.readToken;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenResourceTest {
    private static final ClassLoader LOADER = TokenResourceTest.class.getClassLoader();

    @Test
    public void readsAndStripsInjectedToken() {
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", readToken(LOADER, "test-plugin"));
    }

    @Test
    public void missingTokenReturnsNull() {
        assertNull(readToken(LOADER, "missing"));
    }

    @Test
    public void emptyTokenReturnsNull() {
        assertNull(readToken(LOADER, "empty"));
    }

    @Test
    public void unresolvedPlaceholderReturnsNull() {
        assertNull(readToken(LOADER, "placeholder"));
    }

    @Test
    public void tokensAreNamespacedByIdOnASharedClassLoader() {
        final var shared = new ClassLoader(null) {
            private final Map<String, String> resources = Map.of(
                    "META-INF/faststats/first.token", "11111111111111111111111111111111",
                    "META-INF/faststats/second.token", "22222222222222222222222222222222"
            );

            @Override
            public @Nullable InputStream getResourceAsStream(final String name) {
                final var value = resources.get(name);
                return value == null ? null : new ByteArrayInputStream(value.getBytes(UTF_8));
            }
        };
        assertEquals("11111111111111111111111111111111", readToken(shared, "first"));
        assertEquals("22222222222222222222222222222222", readToken(shared, "second"));
    }

    @Test
    public void contextWithoutTokenIsDisabled() {
        final var context = new MockContext.Factory()
                .noToken()
                .metrics(Metrics.Factory::create)
                .errorTrackerService(ErrorTracker.contextUnaware())
                .featureFlagService(FeatureFlagService.Factory::create)
                .create();

        assertFalse(context.isEnabled());
        assertEquals("", context.getToken());
        assertFalse(context.metrics().isPresent());
        assertFalse(context.errorTrackerService().isPresent());
        assertFalse(context.featureFlagService().isPresent());
        assertDoesNotThrow(context::ready);
        assertDoesNotThrow(context::shutdown);
    }

    @Test
    public void contextWithTokenIsEnabled() {
        final var context = new MockContext.Factory().metrics(Metrics.Factory::create).create();
        assertTrue(context.isEnabled());
        context.shutdown();
    }
}
