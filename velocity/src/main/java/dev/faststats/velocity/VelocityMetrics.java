package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface VelocityMetrics extends Metrics permits VelocityMetricsImpl {
    final class Factory extends VelocityMetricsImpl.Factory {
        public Factory(final VelocityContext context, final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory) {
            super(context, server, logger, dataDirectory);
        }

        /**
         * Creates a new metrics factory for Velocity.
         *
         * @param server        the velocity server
         * @param logger        the logger
         * @param dataDirectory the data directory
         * @apiNote This instance is automatically injected into your plugin.
         * @since 0.1.0
         */
        @Inject
        private Factory(final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory) {
            super(server, logger, dataDirectory);
        }
    }
}
