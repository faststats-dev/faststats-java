package dev.faststats.core.flags;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.faststats.core.Token;
import dev.faststats.core.internal.Logger;
import dev.faststats.core.internal.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
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
    private static final Logger logger = LoggerFactory.factory().getLogger(SimpleFeatureFlagService.class.getName());
    private static final URI defaultUrl = URI.create("https://flags.faststats.dev/v1");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final @Token String token;
    private final @Nullable Attributes attributes;
    private final Duration ttl;
    private final URI url;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> fetchTimes = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<?>> fetchesInProgress = new ConcurrentHashMap<>();

    SimpleFeatureFlagService(
            final @Token String token,
            final @Nullable Attributes attributes,
            final Duration ttl
    ) throws IllegalArgumentException {
        if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
        this.token = token;
        this.attributes = attributes;
        this.ttl = ttl;
        this.url = getFlagsServerUrl();
    }

    private URI getFlagsServerUrl() {
        final var property = System.getProperty("faststats.flags-server");
        try {
            return property != null ? new URI(property) : defaultUrl;
        } catch (final URISyntaxException e) {
            logger.error("Failed to parse flags server url: %s", e, property);
            return defaultUrl;
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<T> get(final SimpleFeatureFlag<T> flag) {
        return Optional.ofNullable((T) cache.get(flag.getId()));
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> whenReady(final SimpleFeatureFlag<T> flag) {
        final var cached = cache.get(flag.getId());
        if (cached == null || isExpired(flag)) return fetch(flag);
        return CompletableFuture.completedFuture((T) cached);
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
        final var requestBody = new JsonObject();
        requestBody.addProperty("serverId", UUID.randomUUID().toString()); // todo: read from config
        requestBody.addProperty("flag", flag.getId());

        final var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(3))
                .uri(url.resolve(path))
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
        requestBody.addProperty("serverId", UUID.randomUUID().toString()); // todo: read from config
        requestBody.addProperty("key", flag.getId());

        final var attributes = new JsonObject();
        if (this.attributes != null) this.attributes.forEachPrimitive(attributes::add);
        if (flag.attributes() != null) flag.attributes().forEachPrimitive(attributes::add);
        if (!attributes.isEmpty()) requestBody.add("attributes", attributes);

        final var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(3))
                .uri(url.resolve("/check"))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            try {
                final var body = JsonParser.parseString(response.body());

                if (response.statusCode() < 200 && response.statusCode() >= 300)
                    throw new IllegalStateException("Unexpected response status: %s (%s)".formatted(response.statusCode(), body));

                final var value = getValue(flag, body);
                cache.put(flag.getId(), value);
                fetchTimes.put(flag.getId(), System.currentTimeMillis());
                return value;
            } catch (final JsonParseException e) {
                throw new IllegalStateException("Unexpected response body: %s (%s)".formatted(response.body(), response.statusCode()), e);
            }
        }).whenComplete((ignored, throwable) -> fetchesInProgress.remove(flag.getId()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(final SimpleFeatureFlag<T> flag, final JsonElement body) {
        if (!(body instanceof final JsonObject object))
            throw new IllegalStateException("Unexpected JSON response: " + body);
        if (!(object.get("value") instanceof final JsonPrimitive primitive))
            throw new IllegalStateException("Missing or invalid 'value' in JSON response: " + body);

        return (T) switch (flag.getType()) {
            case STRING -> primitive.getAsString();
            case NUMBER -> primitive.getAsNumber();
            case BOOLEAN -> primitive.getAsBoolean();
        };
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
}
