package dev.faststats;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

record SimpleJfrPerformanceOptions(
        Duration duration,
        Set<String> eventNames,
        int maxEvents,
        @Nullable ClassLoader attributionLoader
) implements JfrPerformanceOptions {
    SimpleJfrPerformanceOptions {
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("duration must be greater than zero");
        if (eventNames.isEmpty()) throw new IllegalArgumentException("eventNames must not be empty");
        if (maxEvents < 1) throw new IllegalArgumentException("maxEvents must be greater than zero");
        eventNames = Set.copyOf(eventNames);
    }

    @Override
    public Factory toFactory() {
        return new Factory()
                .duration(duration)
                .eventNames(eventNames)
                .maxEvents(maxEvents)
                .attributionLoader(attributionLoader);
    }

    static final class Factory implements JfrPerformanceOptions.Factory {
        private Duration duration = Duration.ofSeconds(10);
        private Set<String> eventNames = Set.of(
                FlightEvents.EXECUTION_SAMPLE,
                FlightEvents.JAVA_MONITOR_ENTER,
                FlightEvents.SOCKET_READ,
                FlightEvents.SOCKET_WRITE,
                FlightEvents.FILE_READ,
                FlightEvents.FILE_WRITE
        );
        private int maxEvents = 10_000;
        private @Nullable ClassLoader attributionLoader;

        @Override
        public Factory duration(final Duration duration) {
            this.duration = duration;
            return this;
        }

        @Override
        public Factory eventNames(final Set<String> eventNames) {
            this.eventNames = eventNames;
            return this;
        }

        @Override
        public Factory maxEvents(final int maxEvents) {
            this.maxEvents = maxEvents;
            return this;
        }

        @Override
        public Factory attributionLoader(final @Nullable ClassLoader loader) {
            this.attributionLoader = loader;
            return this;
        }

        @Override
        public JfrPerformanceOptions create() {
            return new SimpleJfrPerformanceOptions(duration, eventNames, maxEvents, attributionLoader);
        }
    }
}
