package dev.faststats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

record SimpleSqlPerformanceOptions(
        Duration slowQueryThreshold,
        int maxQueryShapes,
        boolean includeUpdateCounts,
        List<Map.Entry<Pattern, String>> anonymizationEntries
) implements SqlPerformanceOptions {
    SimpleSqlPerformanceOptions {
        if (slowQueryThreshold.isNegative()) throw new IllegalArgumentException("slowQueryThreshold must not be negative");
        if (maxQueryShapes < 1) throw new IllegalArgumentException("maxQueryShapes must be greater than zero");
        anonymizationEntries = List.copyOf(anonymizationEntries);
    }

    @Override
    public Factory toFactory() {
        final var factory = new Factory()
                .slowQueryThreshold(slowQueryThreshold)
                .maxQueryShapes(maxQueryShapes)
                .includeUpdateCounts(includeUpdateCounts);
        anonymizationEntries.forEach(entry -> factory.anonymize(entry.getKey(), entry.getValue()));
        return factory;
    }

    static final class Factory implements SqlPerformanceOptions.Factory {
        private Duration slowQueryThreshold = Duration.ofMillis(100);
        private int maxQueryShapes = 256;
        private boolean includeUpdateCounts = true;
        private final List<Map.Entry<Pattern, String>> anonymizationEntries = new ArrayList<>();

        @Override
        public Factory slowQueryThreshold(final Duration threshold) {
            this.slowQueryThreshold = threshold;
            return this;
        }

        @Override
        public Factory maxQueryShapes(final int maxQueryShapes) {
            this.maxQueryShapes = maxQueryShapes;
            return this;
        }

        @Override
        public Factory includeUpdateCounts(final boolean includeUpdateCounts) {
            this.includeUpdateCounts = includeUpdateCounts;
            return this;
        }

        @Override
        public Factory anonymize(final Pattern pattern, final String replacement) {
            anonymizationEntries.add(Map.entry(pattern, replacement));
            return this;
        }

        @Override
        public SqlPerformanceOptions create() {
            return new SimpleSqlPerformanceOptions(slowQueryThreshold, maxQueryShapes, includeUpdateCounts, anonymizationEntries);
        }
    }
}
