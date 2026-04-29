package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class StackTraceFingerprint {
    private static final int STACK_TRACE_LIMIT = 5;

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

    private static void append(@Nullable final Throwable error, final StringBuilder builder, final Set<Throwable> visited) {
        if (error == null || !visited.add(error)) return;
        
        if (!builder.isEmpty()) builder.append('\n');
        builder.append("e").append(error.getClass().getName());
        
        var frames = 0;
        for (final var element : error.getStackTrace()) {
            if (ErrorHelper.isLibraryFrame(element.getClassName())) continue;
            builder.append("\nf").append(element.getClassName()).append('.').append(element.getMethodName());
            if (++frames >= STACK_TRACE_LIMIT) break;
        }

        append(error.getCause(), builder, visited);
    }
}
