package dev.faststats;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

@ApiStatus.Internal
public abstract class SimpleContext implements FastStatsContext {
    private final Config config;
    private final @Token String token;

    /**
     * Creates a new context that stores the shared configuration and token for all FastStats services.
     *
     * @param config the shared configuration
     * @param token  the FastStats project token
     * @throws IllegalArgumentException if the token is invalid
     * @since 0.23.0
     */
    protected SimpleContext(final Config config, @Token final String token) throws IllegalArgumentException {
        if (!token.matches(Token.PATTERN)) {
            throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
        }
        this.config = config;
        this.token = token;
    }

    @Override
    public final Config getConfig() {
        return config;
    }

    @Override
    public final @Token String getToken() {
        return token;
    }

    @Override
    public final FeatureFlagService featureFlags() {
        return new SimpleFeatureFlagService(config, token, null, Duration.ofMinutes(5));
    }

    @Override
    public final FeatureFlagService featureFlags(final Attributes attributes) {
        return new SimpleFeatureFlagService(config, token, attributes, Duration.ofMinutes(5));
    }

    @Override
    public final FeatureFlagService featureFlags(final Duration ttl) {
        return new SimpleFeatureFlagService(config, token, null, ttl);
    }

    @Override
    public final FeatureFlagService featureFlags(@Nullable final Attributes attributes, final Duration ttl) {
        return new SimpleFeatureFlagService(config, token, attributes, ttl);
    }
}
