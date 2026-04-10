package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.core.ErrorTracker;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
public final class AnonymizationTest {
    private static MockMetrics createMetrics(final ErrorTracker tracker) {
        return new MockMetrics(UUID.randomUUID(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", tracker, false);
    }

    private static JsonObject getError(final MockMetrics metrics) {
        final var data = metrics.createData();
        return data.getAsJsonArray("errors").get(0).getAsJsonObject();
    }

    private static String getErrorMessage(final MockMetrics metrics) {
        return getError(metrics).get("message").getAsString();
    }

    @Test
    public void ipv4Anonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Connection refused at 192.168.1.100");
        assertEquals("Connection refused at [IP hidden]", getErrorMessage(metrics));
    }

    @Test
    public void ipv6Anonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Failed to connect to f833:be65:65da:975b:4896:88f7:6964:44c0");
        assertEquals("Failed to connect to [IP hidden]", getErrorMessage(metrics));
    }

    @Test
    public void userHomePathAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        final var username = System.getProperty("user.name", "user");
        tracker.trackError("File not found: /home/" + username + "/config.yml");
        assertEquals("File not found: /home/[username hidden]/config.yml", getErrorMessage(metrics));
    }

    @Test
    public void windowsUserPathAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        final var username = System.getProperty("user.name", "user");
        tracker.trackError("File not found: C:\\Users\\" + username + "\\config.yml");
        assertEquals("File not found: C:\\Users\\[username hidden]\\config.yml", getErrorMessage(metrics));
    }

    @Test
    public void macUserPathAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        final var username = System.getProperty("user.name", "user");
        tracker.trackError("File not found: /Users/" + username + "/config.yml");
        assertEquals("File not found: /Users/[username hidden]/config.yml", getErrorMessage(metrics));
    }

    @Test
    public void discordWebhookAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Webhook failed: https://discord.com/api/webhooks/1234567890987654321/aAaAaAaa0AAaAAaaaAAAAa_0AAAAAAAaaaAaaAaaAAAA0aA00AAA0AAA0aAAaA0a0a0A");
        assertEquals("Webhook failed: https://discord.com/api/webhooks/1234567890987654321/[token hidden]", getErrorMessage(metrics));
    }

    @Test
    public void jdbcUrlAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Failed: jdbc:mysql://localhost:3306:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://localhost:3306:[password hidden]@mydb", getErrorMessage(metrics));
    }

    @Test
    public void jdbcUrlNoPortAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Failed: jdbc:mysql://mydb.com:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://mydb.com:[password hidden]@mydb", getErrorMessage(metrics));
    }

    @Test
    public void jdbcUrlIpAnonymization() {
        final var tracker = ErrorTracker.contextUnaware();
        final var metrics = createMetrics(tracker);
        tracker.trackError("Failed: jdbc:mysql://127.0.0.1:3306:secretpass@mydb");
        assertEquals("Failed: jdbc:mysql://[IP hidden]:3306:[password hidden]@mydb", getErrorMessage(metrics));
    }

    @Test
    public void customPatternAnonymizesMessage() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("token=[^&]+", "token=[redacted]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("Request failed with token=abc123secret&user=test");
        assertEquals("Request failed with token=[redacted]&user=test", getErrorMessage(metrics));
    }

    @Test
    public void customPatternWithCompiledPattern() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize(Pattern.compile("Bearer [A-Za-z0-9._~+/=-]+"), "Bearer [redacted]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("Auth failed: Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature");
        assertEquals("Auth failed: Bearer [redacted]", getErrorMessage(metrics));
    }

    @Test
    public void customPatternWithCaptureGroupReplacement() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("(api_key=)[^&\\s]+", "$1[redacted]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("GET /data?api_key=sk_live_12345&format=json failed");
        assertEquals("GET /data?api_key=[redacted]&format=json failed", getErrorMessage(metrics));
    }

    @Test
    public void multipleCustomPatterns() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("Bearer [A-Za-z0-9._~+/=-]+", "Bearer [redacted]")
                .anonymize("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[email hidden]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("Auth failed for user@example.com with Bearer eyJ0eXAi");
        assertEquals("Auth failed for [email hidden] with Bearer [redacted]", getErrorMessage(metrics));
    }

    @Test
    public void customPatternChaining() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("secret-[a-z]+", "[secret hidden]")
                .anonymize("AKIA[0-9A-Z]{16}", "[aws-key hidden]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("Credentials: secret-abcdef / AKIA1234567890ABCDEF");
        assertEquals("Credentials: [secret hidden] / [aws-key hidden]", getErrorMessage(metrics));
    }

    @Test
    public void customPatternAppliedToCauseChain() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("ssn=\\d{3}-\\d{2}-\\d{4}", "ssn=[redacted]");
        final var metrics = createMetrics(tracker);
        final var cause = new IllegalArgumentException("Validation failed for ssn=123-45-6789");
        tracker.trackError(new RuntimeException("Processing error", cause));
        final var error = getError(metrics);
        final var stack = error.getAsJsonArray("stack");
        var causeAnonymized = false;
        for (final var element : stack) {
            final var line = element.getAsString();
            assertFalse(line.contains("123-45-6789"));
            if (line.startsWith("Caused by:") && line.contains("ssn=[redacted]")) causeAnonymized = true;
        }
        assertTrue(causeAnonymized);
    }

    @Test
    public void nullMessageNotAffected() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("anything", "[redacted]");
        final var metrics = createMetrics(tracker);
        tracker.trackError(new RuntimeException((String) null));
        assertFalse(getError(metrics).has("message"));
    }

    @Test
    public void customAndBuiltInPatternsCombined() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("session=[a-f0-9]+", "session=[redacted]");
        final var metrics = createMetrics(tracker);
        final var username = System.getProperty("user.name", "user");
        tracker.trackError("Error for 192.168.1.1 with session=deadbeef01 at /home/" + username + "/app");
        assertEquals("Error for [IP hidden] with session=[redacted] at /home/[username hidden]/app", getErrorMessage(metrics));
    }

    @Test
    public void emptyReplacementRemovesMatch() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("\\(internal ref: [^)]+\\)", "");
        final var metrics = createMetrics(tracker);
        tracker.trackError("Request failed (internal ref: REF-98765)");
        assertEquals("Request failed ", getErrorMessage(metrics));
    }

    @Test
    public void patternDoesNotMatchLeavesMessageUnchanged() {
        final var tracker = ErrorTracker.contextUnaware()
                .anonymize("SECRET_[A-Z]+", "[redacted]");
        final var metrics = createMetrics(tracker);
        tracker.trackError("just a normal error");
        assertEquals("just a normal error", getErrorMessage(metrics));
    }
}
