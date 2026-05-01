package dev.faststats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.faststats.internal.Logger;
import dev.faststats.internal.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleFeatureFlagService implements FeatureFlagService {
    private static final Logger logger = LoggerFactory.factory().getLogger(SimpleFeatureFlagService.class);
    private static final URI url = getFlagsServerUrl();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final UUID serverId;

    private final @Token String token;
    private final @Nullable Attributes attributes;
    private final Duration ttl;

    private final Map<String, CompletableFuture<?>> fetchesInProgress = new ConcurrentHashMap<>();

    SimpleFeatureFlagService(
            final Config config,
            final @Token String token,
            final @Nullable Attributes attributes,
            final Duration ttl
    ) throws IllegalArgumentException {
        if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
        this.token = token;
        this.attributes = attributes;
        this.ttl = ttl;
        this.serverId = config.serverId();
    }

    private static URI getFlagsServerUrl() {
        final var property = System.getProperty("faststats.flags-server");
        if (property != null) try {
            return new URI(property);
        } catch (final URISyntaxException e) {
            logger.error("Failed to parse flags server url: %s", e, property);
        }
        return URI.create("https://flags.faststats.dev");
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> fetch(final SimpleFeatureFlag<T> flag) {
        return (CompletableFuture<T>) fetchesInProgress.computeIfAbsent(flag.getId(), ignored -> createFetch(flag));
    }

    private <T> CompletableFuture<T> createFetch(final SimpleFeatureFlag<T> flag) {
        final var requestBody = new JsonObject();
        requestBody.addProperty("identifier", serverId.toString());
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
                .uri(url.resolve("/v1/check"))
                .build();

        logger.info("Fetching %s: %s", request.uri(), requestBody.toString());
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            try {
                final var body = JsonParser.parseString(response.body());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    logger.warn("Unexpected response status: %s (%s)", response.statusCode(), body);
                    throw new IllegalStateException("Unexpected response status: %s (%s)".formatted(response.statusCode(), body));
                }

                final var value = getValue(flag, body);
                logger.info("Fetch returned body: %s (value: %s)", body, value);
                flag.setLastFetch(System.currentTimeMillis());
                flag.setValue(value);
                return value;
            } catch (final JsonParseException e) {
                logger.error("Unexpected response body: %s (%s)", e, response.body(), response.statusCode());
                throw new IllegalStateException("Unexpected response body: %s (%s)".formatted(response.body(), response.statusCode()), e);
            }
        }).whenComplete((ignored, throwable) -> fetchesInProgress.remove(flag.getId()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(final SimpleFeatureFlag<T> flag, final JsonElement body) {
        if (!(body instanceof final JsonObject object)) {
            logger.warn("Unexpected JSON response: %s", body);
            throw new IllegalStateException("Unexpected JSON response: " + body);
        }
        if (!(object.get("value") instanceof final JsonPrimitive primitive)) {
            logger.warn("Missing or invalid 'value' in JSON response: %s", body);
            throw new IllegalStateException("Missing or invalid 'value' in JSON response: " + body);
        }

        return (T) switch (flag.getType()) {
            case STRING -> primitive.getAsString();
            case NUMBER -> getAsNumber(primitive);
            case BOOLEAN -> getAsBoolean(primitive);
        };
    }

    private static Number getAsNumber(final JsonPrimitive primitive) {
        try {
            if (primitive.isNumber()) return primitive.getAsNumber();
            return new BigDecimal(primitive.getAsString());
        } catch (final NumberFormatException e) {
            logger.warn("Expected a number but got: %s", primitive.getAsString());
            throw new IllegalStateException("Expected a number but got: " + primitive.getAsString(), e);
        }
    }

    private static boolean getAsBoolean(final JsonPrimitive primitive) {
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        return switch (primitive.getAsString()) {
            case "true" -> true;
            case "false" -> false;
            default -> {
                logger.warn("Expected a boolean but got: %s", primitive.getAsString());
                throw new IllegalStateException("Expected a boolean but got: " + primitive.getAsString());
            }
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
    public Optional<Attributes> getAttributes() {
        return Optional.ofNullable(attributes);
    }

    @Override
    public Duration getTTL() {
        return ttl;
    }

    @Override
    public void shutdown() {
        fetchesInProgress.values().forEach(fetch -> fetch.cancel(true));
        fetchesInProgress.clear();
    }
}
