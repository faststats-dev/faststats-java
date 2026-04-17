package dev.faststats.core;

import org.jspecify.annotations.Nullable;

import java.net.URI;

record SimpleSettings(@Token String token, URI url, boolean debug) implements Settings {

    static final class Factory implements Settings.Factory {
        private static final URI DEFAULT_URL = URI.create("https://metrics.faststats.dev/v1/collect");

        private URI url = DEFAULT_URL;
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
        public Settings.Factory url(final URI url) {
            this.url = url;
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
            return new SimpleSettings(token, url, debug);
        }
    }
}
