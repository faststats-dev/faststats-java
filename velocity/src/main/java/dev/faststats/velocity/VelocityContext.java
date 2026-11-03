package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.config.CustomConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Velocity FastStats context.
 *
 * @since 0.24.0
 */
public final class VelocityContext extends SimpleContext {
    private final Set<ScheduledTask> tasks = new CopyOnWriteArraySet<>();
    final PluginContainer plugin;
    final ProxyServer server;

    private VelocityContext(
            final Factory factory,
            final LoggerFactory loggerFactory,
            final PluginContainer plugin,
            final ProxyServer server,
            @DataDirectory final Path dataDirectory,
            @Token final String token
    ) {
        super(factory, loggerFactory, CustomConfig.builder(dataDirectory.resolveSibling("faststats")
                .resolve("config.properties"), loggerFactory
        ).build(), "velocity", token);
        this.plugin = plugin;
        this.server = server;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new VelocityMetricsImpl(this);
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((CustomConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return plugin.getDescription().getId();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        final var scheduledTask = server.getScheduler()
                .buildTask(plugin, task)
                .delay(Duration.ofMillis(unit.toMillis(initialDelay)))
                .repeat(Duration.ofMillis(unit.toMillis(period)))
                .schedule();
        tasks.add(scheduledTask);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.24.0
     */
    public static class Factory extends SimpleContext.Factory<VelocityContext, Factory> {
        private final PluginContainer plugin;
        private final ProxyServer server;
        private final Path dataDirectory;
        private final Logger logger;
        private @Token
        @Nullable String token;

        /**
         * Creates a new Velocity context builder.
         *
         * @param server        the velocity server
         * @param logger        the plugin logger
         * @param dataDirectory the plugin data directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.24.0
         */
        @Inject
        public Factory(
                final PluginContainer plugin,
                final ProxyServer server,
                final Logger logger,
                @DataDirectory final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.server = server;
            this.logger = logger;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Sets the FastStats project token used by the context created from this factory.
         *
         * @param token the FastStats project token
         * @return this factory
         * @throws IllegalArgumentException if the token is invalid
         * @since 0.24.0
         */
        public Factory token(@Token final String token) {
            this.token = token;
            return this;
        }

        @Override
        public VelocityContext create() {
            if (token == null) throw new IllegalStateException("Token not configured");
            final var loggerFactory = new PlatformLoggerFactory((level, t, message) -> {
                switch (level) {
                    case INFO -> logger.info(message, t);
                    case ERROR -> logger.error(message, t);
                    case WARN -> logger.warn(message, t);
                }
            });
            return new VelocityContext(this, loggerFactory, plugin, server, dataDirectory, token);
        }
    }

    /**
     * Injectable Velocity context builder.
     *
     * @since 0.24.0
     */
    public static final class Builder extends Factory {
        @Inject
        public Builder(
                final PluginContainer plugin,
                final ProxyServer server,
                final Logger logger,
                @DataDirectory final Path dataDirectory
        ) {
            super(plugin, server, logger, dataDirectory);
        }
    }
}
