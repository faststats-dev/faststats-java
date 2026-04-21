package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class StackTraceFingerprint {
    private StackTraceFingerprint() {
    }

    public static String hash(final Throwable error) {
        return MurmurHash3.hash(normalize(error));
    }

    public static String normalize(final Throwable error) {
        final var visited = Collections.<Throwable>newSetFromMap(new IdentityHashMap<>());
        final var builder = new StringBuilder();
        append(error, builder, visited);
        return builder.toString();
    }

    private static void append(@Nullable final Throwable error, final StringBuilder builder,
                               final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return;

        append(builder, error.getClass().getName() + ": " + error.getMessage());
        for (final var element : error.getStackTrace()) {
            if (ErrorHelper.isLibraryClass(element.getClassName())) continue;
            append(builder, " " + element.getClassName() + "." + element.getMethodName());
        }

        append(error.getCause(), builder, visited);
    }

    private static void append(final StringBuilder builder, final String value) {
        if (!builder.isEmpty()) builder.append('\n');
        builder.append(value);
    }
}
