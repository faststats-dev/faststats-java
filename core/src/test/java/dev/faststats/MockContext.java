package dev.faststats;

import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MockContext extends SimpleContext {
    private final Factory factory;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "faststats-submitter");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();

    @SuppressWarnings("PatternValidation")
    private MockContext(final Factory factory, final LoggerFactory loggerFactory) throws IllegalArgumentException {
        super(factory, loggerFactory, factory.config(), "test", factory.resolveToken(factory.token, MockContext.class.getClassLoader(), "mock"));
        this.factory = factory;
        initializeServices(factory);
    }

    @Override
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new MockMetrics(this);
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return !factory.firstRun;
    }

    @Override
    public String getProjectName() {
        return "Mock";
    }

    @Override
    public void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(executor.scheduleAtFixedRate(task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdown();
    }

    private record MockConfig(
            UUID serverId,
            boolean enabled,
            boolean submitMetrics,
            boolean errorTracking,
            boolean additionalMetrics,
            boolean debug
    ) implements Config {
        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public boolean submitMetrics() {
            return submitMetrics;
        }

        @Override
        public boolean errorTracking() {
            return errorTracking;
        }

        @Override
        public boolean additionalMetrics() {
            return additionalMetrics;
        }

        @Override
        public boolean debug() {
            return debug;
        }
    }

    public static final class Factory extends SimpleContext.Factory<MockContext, Factory> {
        private Config config = new MockConfig(UUID.randomUUID(), true, true, true, true, true);
        private boolean firstRun = false;
        @org.jspecify.annotations.Nullable
        private @Token String token = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        public Factory noToken() {
            token = null;
            return this;
        }

        public Factory allFeaturesDisabled() {
            config = new MockConfig(config.serverId(), false, false, false, false, false);
            return this;
        }

        public Factory firstRun() {
            firstRun = true;
            return this;
        }

        @Override
        public MockContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                final var output = level == Logger.LogLevel.ERROR ? System.err : System.out;
                output.println("[" + level.name() + "] " + message);
                if (throwable != null) throwable.printStackTrace(output);
            });
            return new MockContext(this, loggerFactory);

        }

        private Config config() {
            return config;
        }
    }
}
