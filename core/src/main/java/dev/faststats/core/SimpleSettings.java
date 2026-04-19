package dev.faststats.core;

import org.jspecify.annotations.Nullable;

import java.net.URI;

record SimpleSettings(@Token String token, URI metricsUrl, URI flagsUrl, boolean debug) implements Settings {

    static final class Factory implements Settings.Factory {
        private URI metricsUrl = URI.create("https://metrics.faststats.dev/v1");
        private URI flagsUrl = URI.create("https://flags.faststats.dev/v1");
        private @Nullable String token;
        private boolean debug = false;

        @Override
        public Settings.Factory token(@Token final String token) throws IllegalArgumentException {
            if (!token.matches(Token.PATTERN)) {
                throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
            }
            this.token = token;
            return this;
        }

        @Override
        public Settings.Factory metricsServer(final URI url) {
            this.metricsUrl = url;
            return this;
        }

        @Override
        public Settings.Factory flagsServer(final URI url) {
            this.flagsUrl = url;
            return this;
        }

        @Override
        public Settings.Factory debug(final boolean enabled) {
            this.debug = enabled;
            return this;
        }

        @Override
        @SuppressWarnings("PatternValidation")
        public Settings create() throws IllegalStateException {
            if (token == null) throw new IllegalStateException("Token must be specified");
            return new SimpleSettings(token, metricsUrl, flagsUrl, debug);
        }
    }
}
