package dev.faststats.core.flags;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.faststats.core.Settings;
import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleFeatureFlagService implements FeatureFlagService {
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final Settings settings;
    private final @Nullable Attributes attributes;
    private final Duration ttl;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> fetchTimes = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<?>> fetchesInProgress = new ConcurrentHashMap<>();

    SimpleFeatureFlagService(
            final Settings settings,
            final @Nullable Attributes attributes,
            final Duration ttl
    ) {
        this.settings = settings;
        this.attributes = attributes;
        this.ttl = ttl;
    }

    @SuppressWarnings("unchecked")
    <T> Optional<T> get(final SimpleFeatureFlag<T> flag) {
        final var cached = cache.get(flag.getId());
        return Optional.ofNullable((T) cached);
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> whenReady(final SimpleFeatureFlag<T> flag) {
        final var cached = cache.get(flag.getId());
        if (cached != null && !isExpired(flag)) {
            return CompletableFuture.completedFuture((T) cached);
        }
        return fetch(flag);
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> fetch(final SimpleFeatureFlag<T> flag) {
        return (CompletableFuture<T>) fetchesInProgress.computeIfAbsent(flag.getId(), ignored -> createFetch(flag));
    }

    <T> CompletableFuture<T> optIn(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, "/opt-in");
    }

    <T> CompletableFuture<T> optOut(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, "/opt-out");
    }

    private <T> CompletableFuture<T> sendOptRequest(final SimpleFeatureFlag<T> flag, final String path) {
        invalidate(flag);

        final var requestBody = new JsonObject();
        requestBody.addProperty("projectToken", settings.token());
        requestBody.addProperty("serverId", UUID.randomUUID().toString()); // todo: read from config
        requestBody.addProperty("flag", flag.getId());

        final var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + settings.token())
                .timeout(Duration.ofSeconds(3))
                .uri(settings.flagsUrl().resolve(path))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Feature flag opt request failed with status " + response.statusCode()
                ));
            }
            return fetch(flag);
        });
    }

    <T> void invalidate(final SimpleFeatureFlag<T> flag) {
        final var id = flag.getId();
        cache.remove(id);
        fetchTimes.remove(id);
    }

    Optional<Instant> getExpiration(final SimpleFeatureFlag<?> flag) {
        final var lastFetch = fetchTimes.get(flag.getId());
        if (lastFetch == null) return Optional.empty();
        return Optional.of(Instant.ofEpochMilli(lastFetch).plus(ttl));
    }

    boolean isValid(final SimpleFeatureFlag<?> flag) {
        return cache.containsKey(flag.getId()) && !isExpired(flag);
    }

    boolean isExpired(final SimpleFeatureFlag<?> flag) {
        final var lastFetch = fetchTimes.get(flag.getId());
        if (lastFetch == null) return true;
        return System.currentTimeMillis() - lastFetch > ttl.toMillis();
    }

    private <T> CompletableFuture<T> createFetch(final SimpleFeatureFlag<T> flag) {
        final var requestBody = new JsonObject();
        requestBody.addProperty("flag", flag.getId());

        final var mergedAttributes = Attributes.join(attributes, flag.attributes());
        // todo: drop gson
        requestBody.add("attributes", GSON.toJsonTree(mergedAttributes));

        final var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + settings.token())
                .timeout(Duration.ofSeconds(3))
                .uri(settings.flagsUrl().resolve("/check"))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // todo: replace gson with safer read
                final var body = GSON.fromJson(response.body(), JsonObject.class);
                final var value = body.get("value");
                if (value != null && !value.isJsonNull()) {
                    cache.put(flag.getId(), toValue(value));
                    fetchTimes.put(flag.getId(), System.currentTimeMillis());
                    return flag.getType().cast(cache.get(flag.getId()));
                }
            }
            return flag.getDefaultValue();
        }).whenComplete((ignored, throwable) -> fetchesInProgress.remove(flag.getId()));
    }

    private Object toValue(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            final var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsNumber();
            return primitive.getAsString();
        } // todo: guarantee for primitives?
        return element.toString();
    }

    @Override
    public FeatureFlag<Boolean> define(final String id, final boolean defaultValue) {
        return new SimpleFeatureFlag<>(id, defaultValue, null, this);
    }

    @Override
    public FeatureFlag<Boolean> define(final String id, final boolean defaultValue, final Attributes attributes) {
        return new SimpleFeatureFlag<>(id, defaultValue, attributes, this);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue) {
        return new SimpleFeatureFlag<>(id, defaultValue, null, this);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue, final Attributes attributes) {
        return new SimpleFeatureFlag<>(id, defaultValue, attributes, this);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue) {
        return new SimpleFeatureFlag<>(id, defaultValue, null, this);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue, final Attributes attributes) {
        return new SimpleFeatureFlag<>(id, defaultValue, attributes, this);
    }

    @Override
    public void shutdown() {
        cache.clear();
        fetchTimes.clear();
        fetchesInProgress.clear();
    }

    static final class Factory implements FeatureFlagService.Factory {
        private Duration ttl = Duration.ofMinutes(5);
        private @Nullable Settings settings;
        private @Nullable Attributes attributes;

        @Override
        public FeatureFlagService.Factory ttl(final Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        @Override
        public FeatureFlagService.Factory attributes(final Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        @Override
        public FeatureFlagService.Factory settings(final Settings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public FeatureFlagService create() throws IllegalStateException {
            if (settings == null) throw new IllegalStateException("Settings must be specified");
            return new SimpleFeatureFlagService(settings, attributes, ttl);
        }
    }
}
