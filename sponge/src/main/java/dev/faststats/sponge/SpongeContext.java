package dev.faststats.sponge;

import com.google.inject.Inject;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import dev.faststats.Token;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Sponge FastStats context.
 *
 * @since 0.24.0
 */
public final class SpongeContext extends SimpleContext {
    private final Set<ScheduledTask> tasks = new CopyOnWriteArraySet<>();
    final PluginContainer plugin;

    private SpongeContext(
            final Factory factory,
            final LoggerFactory loggerFactory,
            final PluginContainer plugin,
            @ConfigDir(sharedRoot = true) final Path dataDirectory,
            @Token final @Nullable String token
    ) {
        super(factory, loggerFactory, SpongeConfig.read(plugin, dataDirectory
                .resolve("faststats").resolve("config.properties"), loggerFactory
        ), "sponge", token);
        this.plugin = plugin;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected Metrics.Factory metricsFactory() {
        return new SimpleMetrics.Factory(this) {
            @Override
            public Metrics create() throws IllegalStateException {
                return new SpongeMetricsImpl(this);
            }
        };
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SpongeConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return plugin.metadata().id();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        tasks.add(Sponge.asyncScheduler().submit(Task.builder()
                .delay(initialDelay, unit)
                .interval(period, unit)
                .plugin(plugin)
                .execute(task)
                .build()));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    /**
     * Injectable Sponge context builder.
     *
     * @since 0.24.0
     */
    public static class Factory extends SimpleContext.Factory<SpongeContext, Factory> {
        private final PluginContainer plugin;
        private final Path dataDirectory;
        @Nullable
        private @Token String token;

        /**
         * Creates a new Sponge context builder.
         *
         * @param plugin        the plugin container
         * @param dataDirectory the shared Sponge config directory
         * @apiNote This instance can be injected into your plugin.
         * @since 0.24.0
         */
        @Inject
        public Factory(
                final PluginContainer plugin,
                @ConfigDir(sharedRoot = true) final Path dataDirectory
        ) {
            this.plugin = plugin;
            this.dataDirectory = dataDirectory;
        }

        /**
         * Sets the FastStats project token used by the context created from this factory.
         * <p>
         * If no token is set, the token injected at build time is read
         * from {@code META-INF/faststats/<plugin id>.token}.
         * If no token was injected, a warning is logged and the created context is disabled.
         *
         * @param token the FastStats project token
         * @return this factory
         * @throws IllegalArgumentException if the token is invalid
         * @since 0.24.0
         */
        public SpongeContext.Factory token(@Token final String token) throws IllegalArgumentException {
            this.token = token;
            return this;
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public SpongeContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                switch (level) {
                    case INFO -> {
                        if (throwable == null) plugin.logger().info(message);
                        else plugin.logger().info(message, throwable);
                    }
                    case ERROR -> {
                        if (throwable == null) plugin.logger().error(message);
                        else plugin.logger().error(message, throwable);
                    }
                    case WARN -> {
                        if (throwable == null) plugin.logger().warn(message);
                        else plugin.logger().warn(message, throwable);
                    }
                }
            });
            final var token = resolveToken(this.token, plugin.instance().getClass().getClassLoader(), plugin.metadata().id());
            return new SpongeContext(this, loggerFactory, plugin, dataDirectory, token);
        }
    }

    /**
     * Injectable Sponge context builder.
     *
     * @since 0.24.0
     */
    public static final class Builder extends Factory {
        @Inject
        public Builder(
                final PluginContainer plugin,
                @ConfigDir(sharedRoot = true) final Path dataDirectory
        ) {
            super(plugin, dataDirectory);
        }
    }
}
