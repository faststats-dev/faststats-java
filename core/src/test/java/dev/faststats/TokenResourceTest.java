package dev.faststats;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TokenResourceTest {
    private static final ClassLoader LOADER = TokenResourceTest.class.getClassLoader();

    @Test
    public void readsAndStripsInjectedToken() {
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", SimpleContext.Factory.readToken(LOADER, "test-plugin"));
    }

    @Test
    public void missingTokenThrows() {
        final var error = assertThrows(IllegalStateException.class, () -> SimpleContext.Factory.readToken(LOADER, "missing"));
        assertTrue(error.getMessage().contains("META-INF/faststats/missing.token"), error.getMessage());
    }

    @Test
    public void emptyTokenThrows() {
        final var error = assertThrows(IllegalStateException.class, () -> SimpleContext.Factory.readToken(LOADER, "empty"));
        assertTrue(error.getMessage().contains("is empty"), error.getMessage());
    }

    @Test
    public void tokensAreNamespacedByIdOnASharedClassLoader() {
        final var shared = new ClassLoader(null) {
            private final Map<String, String> resources = Map.of(
                    "META-INF/faststats/first.token", "11111111111111111111111111111111",
                    "META-INF/faststats/second.token", "22222222222222222222222222222222"
            );

            @Override
            public InputStream getResourceAsStream(final String name) {
                return new ByteArrayInputStream(resources.get(name).getBytes(UTF_8));
            }
        };
        assertEquals("11111111111111111111111111111111", SimpleContext.Factory.readToken(shared, "first"));
        assertEquals("22222222222222222222222222222222", SimpleContext.Factory.readToken(shared, "second"));
    }
}
