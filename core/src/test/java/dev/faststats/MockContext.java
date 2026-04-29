package dev.faststats;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public final class MockContext extends SimpleContext {
    public MockContext(final UUID serverId, @Token final String token, final boolean debug) throws IllegalArgumentException {
        super(new MockConfig(serverId, debug), token);
    }

    @Override
    public Metrics.Factory metrics() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new MockMetrics(this);
            }
        };
    }

    private record MockConfig(UUID serverId, boolean debug) implements dev.faststats.Config {
        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean errorTracking() {
            return true;
        }

        @Override
        public boolean additionalMetrics() {
            return true;
        }
    }
}
