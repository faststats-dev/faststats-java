package dev.faststats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class SimpleFeatureFlagService extends SubmissionService implements FeatureFlagService {
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final String CHECK_PATH = "/v1/check";
    private static final String OPT_IN_PATH = "/v1/opt-in";
    private static final String OPT_OUT_PATH = "/v1/opt-out";

    private final URI url = getServerUrl("faststats.flags-server", "https://flags.faststats.dev");

    private final SimpleContext context;

    private final Attributes attributes;
    private final Duration ttl;

    private final Map<String, CompletableFuture<?>> fetchesInProgress = new ConcurrentHashMap<>();

    SimpleFeatureFlagService(
            final SimpleContext context,
            final Attributes attributes,
            final Duration ttl
    ) throws IllegalArgumentException {
        super(context);
        if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
        this.attributes = attributes;
        this.context = context;
        this.ttl = ttl;
    }

    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> fetch(final SimpleFeatureFlag<T> flag) {
        final var future = new CompletableFuture<T>();
        final var existing = fetchesInProgress.putIfAbsent(flag.getId(), future);
        if (existing != null) return (CompletableFuture<T>) existing;
        createFetch(flag).whenComplete((value, throwable) -> {
            fetchesInProgress.remove(flag.getId(), future);
            if (throwable != null) future.completeExceptionally(throwable);
            else future.complete(value);
        });
        return future;
    }

    <T> CompletableFuture<T> optIn(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, OPT_IN_PATH);
    }

    <T> CompletableFuture<T> optOut(final SimpleFeatureFlag<T> flag) {
        return sendOptRequest(flag, OPT_OUT_PATH);
    }

    private <T> CompletableFuture<T> sendOptRequest(final SimpleFeatureFlag<T> flag, final String path) {
        final var requestBody = createRequestBody(flag);
        return send(path, requestBody).thenCompose(response -> {
            if (isSuccessful(response)) return fetch(flag);
            logUnsuccessfulResponse(response);
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Feature flag opt request failed with status %s (%s)".formatted(response.statusCode(), response.body())
            ));
        });
    }

    private <T> CompletableFuture<T> createFetch(final SimpleFeatureFlag<T> flag) {
        final var requestBody = createRequestBody(flag);
        final var request = createRequest(CHECK_PATH, requestBody);
        logger.info("Fetching %s: %s", request.uri(), requestBody.toString());
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleFetchResponse(flag, response));
    }

    private JsonObject createRequestBody(final SimpleFeatureFlag<?> flag) {
        final var requestBody = new JsonObject();
        final var attributes = new JsonObject();

        requestBody.addProperty("identifier", context.getConfig().serverId().toString());
        requestBody.addProperty("key", flag.getId());

        this.attributes.forEachPrimitive(attributes::add);
        if (flag.attributes() != null) flag.attributes().forEachPrimitive(attributes::add);
        if (!attributes.isEmpty()) requestBody.add("attributes", attributes);

        return requestBody;
    }

    private CompletableFuture<HttpResponse<String>> send(final String path, final JsonObject requestBody) {
        return HTTP_CLIENT.sendAsync(createRequest(path, requestBody), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest createRequest(final String path, final JsonObject requestBody) {
        return HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + context.getToken())
                .timeout(TIMEOUT)
                .uri(url.resolve(path))
                .build();
    }

    private <T> T handleFetchResponse(final SimpleFeatureFlag<T> flag, final HttpResponse<String> response) {
        try {
            if (!isSuccessful(response)) {
                logUnsuccessfulResponse(response);
                throw new IllegalStateException("Unexpected response status: %s (%s)".formatted(response.statusCode(), response.body()));
            }

            final var body = JsonParser.parseString(response.body());
            final var value = getValue(flag, body);
            logger.info("Fetch returned body: %s (value: %s)", body, value);
            flag.setLastFetch(System.currentTimeMillis());
            flag.setValue(value);
            return value;
        } catch (final JsonParseException e) {
            logger.error("Unexpected response body: %s (%s)", e, response.body(), response.statusCode());
            throw new IllegalStateException("Unexpected response body: %s (%s)".formatted(response.body(), response.statusCode()), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(final SimpleFeatureFlag<T> flag, final JsonElement body) {
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

    private Number getAsNumber(final JsonPrimitive primitive) {
        try {
            if (primitive.isNumber()) return primitive.getAsNumber();
            return new BigDecimal(primitive.getAsString());
        } catch (final NumberFormatException e) {
            logger.warn("Expected a number but got: %s", primitive.getAsString());
            throw new IllegalStateException("Expected a number but got: " + primitive.getAsString(), e);
        }
    }

    private boolean getAsBoolean(final JsonPrimitive primitive) {
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
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<Boolean> define(final String id, final boolean defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue) {
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<String> define(final String id, final String defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue) {
        return defineFlag(id, defaultValue, null);
    }

    @Override
    public FeatureFlag<Number> define(final String id, final Number defaultValue, final Attributes attributes) {
        return defineFlag(id, defaultValue, attributes);
    }

    private <T> FeatureFlag<T> defineFlag(final String id, final T defaultValue, @Nullable final Attributes attributes) {
        return new SimpleFeatureFlag<>(id, defaultValue, attributes, this);
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public Duration getTTL() {
        return ttl;
    }

    public void shutdown() {
        fetchesInProgress.values().forEach(fetch -> fetch.cancel(true));
        fetchesInProgress.clear();
    }

    @Override
    protected String serverType() {
        return "feature flag";
    }

    static final class Factory implements FeatureFlagService.Factory {
        private final SimpleContext context;
        private @Nullable Attributes attributes;
        private Duration ttl = DEFAULT_TTL;

        Factory(final SimpleContext context) {
            this.context = context;
        }

        @Override
        public FeatureFlagService.Factory attributes(final Attributes attributes) {
            this.attributes = attributes;
            return this;
        }

        @Override
        public FeatureFlagService.Factory ttl(final Duration ttl) throws IllegalArgumentException {
            if (ttl.isNegative()) throw new IllegalArgumentException("TTL cannot be negative");
            this.ttl = ttl;
            return this;
        }

        @Override
        public FeatureFlagService create() throws IllegalArgumentException {
            final var attributes = this.attributes != null ? this.attributes : Attributes.empty();
            return new SimpleFeatureFlagService(context, attributes, ttl);
        }
    }
}
