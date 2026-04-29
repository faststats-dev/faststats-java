package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.faststats.internal.Constants;
import org.jspecify.annotations.Nullable;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SimpleErrorTracker implements ErrorTracker {
    private final Map<String, Integer> collected = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> reports = new ConcurrentHashMap<>();

    private final Map<Class<? extends Throwable>, Set<Pattern>> ignoredTypedPatterns = new ConcurrentHashMap<>();
    private final Set<Class<? extends Throwable>> ignoredTypes = new CopyOnWriteArraySet<>();
    private final Set<Pattern> ignoredPatterns = new CopyOnWriteArraySet<>();
    private final List<Map.Entry<Pattern, String>> anonymizationEntries = new CopyOnWriteArrayList<>(List.of(
            Map.entry(ErrorHelper.ipv4Pattern(), "[IP hidden]"),
            Map.entry(ErrorHelper.ipv6Pattern(), "[IP hidden]"),
            Map.entry(ErrorHelper.userHomePathPattern(), "$1$2$3[username hidden]"),
            Map.entry(ErrorHelper.discordWebhookPattern(), "$1[token hidden]"),
            Map.entry(ErrorHelper.jdbcUrlPattern(), "$1[password hidden]$2")
    ));

    private volatile @Nullable BiConsumer<@Nullable ClassLoader, Throwable> errorEvent = null;
    private volatile @Nullable UncaughtExceptionHandler originalHandler = null;

    public SimpleErrorTracker() {
        ErrorHelper.usernamePattern().ifPresent(pattern -> anonymizationEntries.add(Map.entry(pattern, "[username hidden]")));
    }

    @Override
    public void trackError(final String message) {
        trackError(message, true);
    }

    @Override
    public void trackError(final Throwable error) {
        trackError(error, true);
    }

    @Override
    public void trackError(final String message, final boolean handled) {
        trackError(new RuntimeException(message), handled);
    }

    @Override
    public void trackError(final Throwable error, final boolean handled) {
        try {
            if (isIgnored(error, Collections.newSetFromMap(new IdentityHashMap<>()))) return;
            final var hashed = StackTraceFingerprint.hash(error); // todo: report duplicate errors with different messages
            if (collected.compute(hashed, (k, v) -> {
                return v == null ? 1 : v + 1;
            }) > 1) return;
            final var compiled = ErrorHelper.compile(error, null, handled, anonymizationEntries);
            reports.put(hashed, compiled);
        } catch (final NoClassDefFoundError ignored) {
        }
    }

    private boolean isIgnored(@Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return false;

        if (ignoredTypes.contains(error.getClass())) return true;

        final var message = error.getMessage() != null ? error.getMessage() : "";
        if (ignoredPatterns.stream().map(pattern -> pattern.matcher(message)).anyMatch(Matcher::find)) return true;

        final var patterns = ignoredTypedPatterns.get(error.getClass());
        if (patterns != null && patterns.stream().map(pattern -> pattern.matcher(message)).anyMatch(Matcher::find))
            return true;

        return isIgnored(error.getCause(), visited);
    }

    @Override
    public ErrorTracker ignoreError(final Class<? extends Throwable> type) {
        ignoredTypes.add(type);
        return this;
    }

    @Override
    public ErrorTracker ignoreError(final Pattern pattern) {
        ignoredPatterns.add(pattern);
        return this;
    }

    @Override
    public ErrorTracker ignoreError(final Class<? extends Throwable> type, final Pattern pattern) {
        ignoredTypedPatterns.computeIfAbsent(type, k -> new CopyOnWriteArraySet<>()).add(pattern);
        return this;
    }

    @Override
    public ErrorTracker anonymize(final Pattern pattern, final String replacement) {
        anonymizationEntries.add(Map.entry(pattern, replacement));
        return this;
    }

    public JsonArray getData() {
        final var report = new JsonArray(reports.size());

        reports.forEach((hash, object) -> {
            final var count = collected.getOrDefault(hash, 1);
            report.add(fillEntry(object.deepCopy(), hash, count));
        });

        collected.forEach((hash, count) -> {
            if (count <= 0 || reports.containsKey(hash)) return;
            report.add(fillEntry(new JsonObject(), hash, count));
        });

        return report;
    }

    private JsonObject fillEntry(final JsonObject entry, final String hash, final int count) {
        entry.addProperty("hash", hash);
        entry.addProperty("buildId", Constants.BUILD_ID);
        if (count > 1) entry.addProperty("count", count);
        return entry;
    }

    public void clear() {
        collected.replaceAll((k, v) -> 0);
        reports.clear();
    }

    public boolean needsFlushing() {
        if (!reports.isEmpty()) return true;
        for (final var value : collected.values()) {
            if (value > 0) return true;
        }
        return false;
    }

    @Override
    public synchronized void attachErrorContext(@Nullable final ClassLoader loader) throws IllegalStateException {
        if (originalHandler != null) throw new IllegalStateException("Error context already attached");
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            final var handler = originalHandler;
            if (handler != null) handler.uncaughtException(thread, error);
            try {
                if (loader != null && !ErrorTracker.isSameLoader(loader, error)) return;
                final var event = errorEvent;
                if (event != null) event.accept(loader, error);
                trackError(error, false);
            } catch (final Throwable t) {
                trackError(t, false);
            }
        });
    }

    @Override
    public synchronized void detachErrorContext() {
        if (originalHandler == null) return;
        Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        originalHandler = null;
    }

    @Override
    public synchronized boolean isContextAttached() {
        return originalHandler != null;
    }

    @Override
    public synchronized void setContextErrorHandler(@Nullable final BiConsumer<@Nullable ClassLoader, Throwable> errorEvent) {
        this.errorEvent = errorEvent;
    }

    @Override
    public synchronized Optional<BiConsumer<@Nullable ClassLoader, Throwable>> getContextErrorHandler() {
        return Optional.ofNullable(errorEvent);
    }
}
