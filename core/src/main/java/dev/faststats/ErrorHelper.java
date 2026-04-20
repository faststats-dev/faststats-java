package dev.faststats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ErrorHelper {
    private static final int MESSAGE_LENGTH = Math.min(1000, Integer.getInteger("faststats.message-length", 500));
    private static final int STACK_TRACE_LENGTH = Math.min(500, Integer.getInteger("faststats.stack-trace-length", 300));
    private static final int STACK_TRACE_LIMIT = Math.min(50, Integer.getInteger("faststats.stack-trace-limit", 15));

    public static JsonObject compile(final Throwable error, @Nullable final List<String> suppress, final boolean handled,
                                     final List<Map.Entry<Pattern, String>> customPatterns) {
        final var report = new JsonObject();
        final var message = getAnonymizedMessage(error, customPatterns);

        final var stacktrace = new JsonArray();
        final var header = message != null
                ? error.getClass().getName() + ": " + message
                : error.getClass().getName();
        stacktrace.add(header);

        final var elements = error.getStackTrace();
        final var stack = collapseStackTrace(elements);
        final var list = new ArrayList<>(stack);
        if (suppress != null) list.removeAll(suppress);
        final var traces = Math.min(list.size(), STACK_TRACE_LIMIT);

        populateTraces(traces, list, elements, stacktrace);
        appendCauseChain(error.getCause(), stack, suppress, stacktrace, customPatterns);

        report.addProperty("error", error.getClass().getName());
        if (message != null) report.addProperty("message", message);

        report.add("stack", stacktrace);
        report.addProperty("handled", handled);

        return report;
    }

    private static void appendCauseChain(@Nullable Throwable cause, final List<String> parentStack,
                                         @Nullable final List<String> suppress, final JsonArray stacktrace,
                                         final List<Map.Entry<Pattern, String>> customPatterns) {
        final var toSuppress = new ArrayList<>(parentStack);
        if (suppress != null) toSuppress.addAll(suppress);
        final var visited = Collections.<Throwable>newSetFromMap(new IdentityHashMap<>());
        while (cause != null && visited.add(cause)) {
            final var causeMessage = getAnonymizedMessage(cause, customPatterns);
            final var header = causeMessage != null
                    ? "Caused by: " + cause.getClass().getName() + ": " + causeMessage
                    : "Caused by: " + cause.getClass().getName();
            stacktrace.add(header);

            final var causeElements = cause.getStackTrace();
            final var causeStack = collapseStackTrace(causeElements);
            final var causeList = new ArrayList<>(causeStack);
            causeList.removeAll(toSuppress);
            final var causeTraces = Math.min(causeList.size(), STACK_TRACE_LIMIT);
            populateTraces(causeTraces, causeList, causeElements, stacktrace);

            cause = cause.getCause();
        }
    }

    private static void populateTraces(final int traces, final List<String> list, final StackTraceElement[] elements,
                                       final JsonArray stacktrace) {
        for (var i = 0; i < traces; i++) {
            final var string = list.get(i);
            if (string.length() <= STACK_TRACE_LENGTH) stacktrace.add("  at " + string);
            else stacktrace.add("  at " + string.substring(0, STACK_TRACE_LENGTH) + "...");
        }
        if (traces > 0 && traces < list.size()) {
            stacktrace.add("  ... " + (list.size() - traces) + " more");
        } else {
            final var i = elements.length - list.size();
            if (i > 0) stacktrace.add("  ... " + i + " more");
        }
    }

    private static List<String> collapseStackTrace(final StackTraceElement[] trace) {
        final var lines = Arrays.stream(trace)
                .map(StackTraceElement::toString)
                .toList();

        return collapseRepeatingPattern(lines);
    }

    private static List<String> collapseRepeatingPattern(final List<String> lines) {
        final var deduplicated = collapseConsecutiveDuplicates(lines);

        final var n = deduplicated.size();

        for (var cycleLen = 1; cycleLen <= n / 2; cycleLen++) {
            var isPattern = true;
            var repetitions = 0;

            for (var i = 0; i < n; i++) {
                if (!deduplicated.get(i).equals(deduplicated.get(i % cycleLen))) {
                    isPattern = false;
                    break;
                }
                if (i > 0 && i % cycleLen == 0) repetitions++;
            }

            if (isPattern && repetitions >= 2) {
                return deduplicated.subList(0, cycleLen);
            }
        }

        return deduplicated;
    }

    private static List<String> collapseConsecutiveDuplicates(final List<String> lines) {
        if (lines.isEmpty()) return lines;

        final var result = new ArrayList<String>();
        String previous = null;

        for (final var line : lines) {
            if (line.equals(previous)) continue;
            result.add(line);
            previous = line;
        }

        return result;
    }

    public static boolean isSameLoader(final ClassLoader loader, final Throwable error) {
        return isSameLoader(loader, error, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean isSameLoader(final ClassLoader loader, @Nullable final Throwable error, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return false;

        final var stackTrace = error.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0)
            return isSameLoader(loader, error.getCause(), visited);

        final var firstNonLibraryIndex = findFirstNonLibraryFrameIndex(stackTrace);
        if (firstNonLibraryIndex == -1) return isSameLoader(loader, error.getCause(), visited);

        final var framesToCheck = Math.min(5, stackTrace.length - firstNonLibraryIndex);

        for (var i = 0; i < framesToCheck; i++) {
            final var frame = stackTrace[firstNonLibraryIndex + i];
            if (isLibraryClass(frame.getClassName())) continue;
            if (!isFromLoader(frame, loader)) return isSameLoader(loader, error.getCause(), visited);
        }

        return true;
    }

    private static int findFirstNonLibraryFrameIndex(final StackTraceElement[] stackTrace) {
        for (var i = 0; i < stackTrace.length; i++) {
            if (!isLibraryClass(stackTrace[i].getClassName())) return i;
        }
        return -1;
    }

    private static boolean isLibraryClass(final String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("jdk.");
    }

    private static boolean isFromLoader(final StackTraceElement frame, final ClassLoader loader) {
        try {
            final var clazz = Class.forName(frame.getClassName(), false, loader);
            return isSameClassLoader(clazz.getClassLoader(), loader);
        } catch (final Throwable t) {
            return false;
        }
    }

    private static boolean isSameClassLoader(final ClassLoader classLoader, final ClassLoader loader) {
        if (classLoader == loader) return true;
        var current = classLoader;
        while (current != null && current != loader) {
            current = current.getParent();
        }
        return loader == current;
    }

    private static @Nullable String getAnonymizedMessage(final Throwable error, final List<Map.Entry<Pattern, String>> customPatterns) {
        final var message = error.getMessage();
        if (message == null) return null;
        var truncated = message.length() > MESSAGE_LENGTH
                ? message.substring(0, MESSAGE_LENGTH) + "..."
                : message;
        for (final var entry : customPatterns) {
            truncated = entry.getKey().matcher(truncated).replaceAll(entry.getValue());
        }
        return truncated;
    }

    public static Pattern discordWebhookPattern() {
        return Pattern.compile("(https://discord\\.com/api/webhooks/\\d+/)[\\w-]+");
    }

    public static Pattern ipv4Pattern() {
        return Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");
    }

    public static Pattern ipv6Pattern() {
        return Pattern.compile("(?i)\\b([0-9a-f]{1,4}:){7}[0-9a-f]{1,4}\\b|" + // Full form
                "(?i)\\b([0-9a-f]{1,4}:){1,7}:\\b|" +                          // Trailing ::
                "(?i)\\b([0-9a-f]{1,4}:){1,6}:[0-9a-f]{1,4}\\b|" +             // :: in middle (1 group after)
                "(?i)\\b([0-9a-f]{1,4}:){1,5}(:[0-9a-f]{1,4}){1,2}\\b|" +      // :: in middle (2 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,4}(:[0-9a-f]{1,4}){1,3}\\b|" +      // :: in middle (3 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,3}(:[0-9a-f]{1,4}){1,4}\\b|" +      // :: in middle (4 groups after)
                "(?i)\\b([0-9a-f]{1,4}:){1,2}(:[0-9a-f]{1,4}){1,5}\\b|" +      // :: in middle (5 groups after)
                "(?i)\\b[0-9a-f]{1,4}:(:[0-9a-f]{1,4}){1,6}\\b|" +             // :: in middle (6 groups after)
                "(?i)\\b:(:[0-9a-f]{1,4}){1,7}\\b|" +                          // Leading ::
                "(?i)\\b::([0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4}\\b|" +            // :: at start
                "(?i)\\b::\\b");                                               // Just ::
    }

    public static Pattern jdbcUrlPattern() {
        return Pattern.compile("(jdbc:[^:]+://[^:]+:(?:\\d+:)?)[^@]+(@)");
    }

    public static Pattern userHomePathPattern() {
        return Pattern.compile("(/home/)[^/\\s]+" +       // Linux: /home/username
                "|(/Users/)[^/\\s]+" +                    // macOS: /Users/username
                "|((?i)[A-Z]:\\\\Users\\\\)[^\\\\\\s]+"); // Windows: A-Z:\\Users\\username
    }

    public static Optional<Pattern> usernamePattern() {
        return Optional.ofNullable(System.getProperty("user.name"))
                .filter(s -> s.trim().length() > 2)
                .map(Pattern::quote)
                .map(Pattern::compile);
    }
}
