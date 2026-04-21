package dev.faststats.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
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
        public Factory(
                final VelocityContext context,
                final PluginContainer plugin,
                final ProxyServer server,
                final Logger logger,
                @DataDirectory final Path dataDirectory
        ) {
            super(context, plugin, server, logger, dataDirectory);
        }
    }
}
