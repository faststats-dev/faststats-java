package dev.faststats.velocity;

import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.Config;
import dev.faststats.FastStatsContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity FastStats context.
 *
 * @since 0.23.0
 */
public final class VelocityContext extends FastStatsContext {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public VelocityContext(
            final Config config,
            final ProxyServer server,
            final Logger logger,
            final Path dataDirectory,
            @Token final String token
    ) {
        super(config, token);
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public VelocityContext(
            final ProxyServer server,
            final Logger logger,
            @DataDirectory final Path dataDirectory,
            @Token final String token
    ) {
        this(SimpleConfig.read(dataDirectory.resolveSibling("faststats").resolve("config.properties")), server, logger, dataDirectory, token);
    }

    @Override
    public VelocityMetrics.Factory metrics() {
        return new VelocityMetrics.Factory(this, server, logger, dataDirectory);
    }
}
