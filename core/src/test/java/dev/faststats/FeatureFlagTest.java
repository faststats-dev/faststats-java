package dev.faststats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
public final class FeatureFlagTest {
    private static final UUID SERVER_ID = UUID.fromString("76a88a60-1329-4913-9525-fb16b588d07e");
    private static FlagServer server;

    @BeforeAll
    public static void startServer() throws IOException {
        server = new FlagServer();
        System.setProperty("faststats.flags-server", server.url());
    }

    @AfterAll
    public static void stopServer() throws IOException {
        System.clearProperty("faststats.flags-server");
        server.close();
    }

    @BeforeEach
    public void resetServer() {
        server.reset();
    }

    @Test
    public void booleanFlagFetchesAndCachesValue() throws Exception {
        server.enqueue(200, "{\"value\":true}");

        final var service = service(Duration.ofMinutes(5));
        final var flag = service.define("new_commands", false);

        assertEquals("new_commands", flag.getId());
        assertEquals(FeatureFlag.Type.BOOLEAN, flag.getType());
        assertEquals(Boolean.class, flag.getTypeClass());
        assertFalse(flag.getDefaultValue());
        assertEquals(true, flag.whenReady().get(1, TimeUnit.SECONDS));
        assertEquals(Optional.of(true), flag.getCached());
        assertTrue(flag.isValid());
        assertTrue(flag.getExpiration().isPresent());

        final var request = server.takeRequest();
        assertEquals("/v1/check", request.path());
        assertEquals("Bearer test-token", request.headers().get("authorization").getAsString());
        assertEquals(SERVER_ID.toString(), request.body().get("identifier").getAsString());
        assertEquals("new_commands", request.body().get("key").getAsString());
    }

    @Test
    public void stringAndNumberFlagsUseDefaultValueTypes() throws Exception {
        server.enqueue(200, "{\"value\":\"zstd\"}");
        server.enqueue(200, "{\"value\":12.5}");

        final var service = service(Duration.ofMinutes(5));
        final var stringFlag = service.define("compression", "gzip");

        assertEquals(FeatureFlag.Type.STRING, stringFlag.getType());
        assertEquals(String.class, stringFlag.getTypeClass());
        assertEquals("gzip", stringFlag.getDefaultValue());
        assertEquals("zstd", stringFlag.whenReady().get(1, TimeUnit.SECONDS));

        final var numberFlag = service.define("sample_rate", 1);
        assertEquals(FeatureFlag.Type.NUMBER, numberFlag.getType());
        assertEquals(Number.class, numberFlag.getTypeClass());
        assertEquals(1, numberFlag.getDefaultValue());
        assertEquals(12.5, numberFlag.whenReady().get(1, TimeUnit.SECONDS).doubleValue());
    }

    @Test
    public void serviceAndFlagAttributesAreMergedInFetchRequest() throws Exception {
        server.enqueue(200, "{\"value\":true}");

        final var serviceAttributes = Attributes.create()
                .put("region", "global")
                .put("players", 20)
                .put("premium", false);
        final var flagAttributes = Attributes.create()
                .put("region", "flag")
                .put("beta", true);
        final var service = service(serviceAttributes, Duration.ofMinutes(5));
        final var flag = service.define("targeted", false, flagAttributes);

        assertTrue(flag.whenReady().get(1, TimeUnit.SECONDS));

        final var attributes = server.takeRequest().body().getAsJsonObject("attributes");
        assertEquals("flag", attributes.get("region").getAsString());
        assertEquals(20, attributes.get("players").getAsInt());
        assertTrue(attributes.get("beta").getAsBoolean());
        assertFalse(attributes.get("premium").getAsBoolean());
    }

    @Test
    public void whenReadyUsesValidCachedValueWithoutFetchingAgain() throws Exception {
        server.enqueue(200, "{\"value\":true}");

        final var service = service(Duration.ofMinutes(5));
        final var flag = service.define("cached", false);

        assertTrue(flag.whenReady().get(1, TimeUnit.SECONDS));
        server.takeRequest();

        assertTrue(flag.whenReady().get(1, TimeUnit.SECONDS));
        assertEquals(0, server.requestCountAfterWaiting(Duration.ofMillis(150)));
    }

    @Test
    public void whenReadyRefetchesExpiredCachedValue() throws Exception {
        server.enqueue(200, "{\"value\":false}");
        server.enqueue(200, "{\"value\":true}");

        final var service = service(Duration.ofMillis(1));
        final var flag = service.define("expired", false);

        assertFalse(flag.whenReady().get(1, TimeUnit.SECONDS));
        server.takeRequest();
        Thread.sleep(5);

        assertTrue(flag.whenReady().get(1, TimeUnit.SECONDS));
        assertEquals("/v1/check", server.takeRequest().path());
        assertEquals(Optional.of(true), flag.getCached());
        Thread.sleep(5);
        assertFalse(flag.isValid());
    }

    @Test
    public void concurrentFetchesShareInProgressRequest() throws Exception {
        final var releaseResponse = new CountDownLatch(1);
        server.enqueue(200, "{\"value\":true}", releaseResponse);

        final var service = service(Duration.ofMinutes(5));
        final var flag = service.define("shared", false);

        final CompletableFuture<Boolean> first = flag.fetch();
        final CompletableFuture<Boolean> second = flag.fetch();

        assertSame(first, second);
        assertEquals(1, server.requestCountAfterWaiting(Duration.ofMillis(150)));

        releaseResponse.countDown();
        assertTrue(first.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void nonSuccessfulFetchResponseFails() {
        server.enqueue(500, "{\"value\":true}");

        final var service = service(Duration.ofMinutes(5));
        final var flag = service.define("broken", false);

        final var error = assertThrows(Exception.class, () -> flag.whenReady().get(1, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    private static SimpleFeatureFlagService service(final Duration ttl) {
        return service(null, ttl);
    }

    private static SimpleFeatureFlagService service(@Nullable final Attributes attributes, final Duration ttl) {
        return new SimpleFeatureFlagService(new TestConfig(), "test-token", attributes, ttl);
    }

    private record TestConfig() implements Config {
        @Override
        public UUID serverId() {
            return SERVER_ID;
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean errorTracking() {
            return true;
        }

        @Override
        public boolean additionalMetrics() {
            return true;
        }

        @Override
        public boolean debug() {
            return false;
        }
    }

    private static final class FlagServer implements AutoCloseable {
        private final ServerSocket socket;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final LinkedBlockingQueue<Response> responses = new LinkedBlockingQueue<>();
        private final LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<>();

        FlagServer() throws IOException {
            socket = new ServerSocket(0);
            executor.execute(() -> {
                while (!socket.isClosed()) {
                    try {
                        final var client = socket.accept();
                        executor.execute(() -> handle(client));
                    } catch (final IOException e) {
                        if (!socket.isClosed()) throw new UncheckedIOException(e);
                    }
                }
            });
        }

        String url() {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }

        void enqueue(final int status, final String body) {
            enqueue(status, body, null);
        }

        void enqueue(final int status, final String body, @Nullable final CountDownLatch release) {
            responses.add(new Response(status, body, release));
        }

        void reset() {
            requests.clear();
            responses.clear();
        }

        Request takeRequest() throws InterruptedException {
            final var request = requests.poll(1, TimeUnit.SECONDS);
            if (request == null) throw new AssertionError("Timed out waiting for request");
            return request;
        }

        int requestCountAfterWaiting(final Duration duration) throws InterruptedException {
            Thread.sleep(duration.toMillis());
            return requests.size();
        }

        private void handle(final Socket client) {
            try (client) {
                final var reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                final var requestLine = reader.readLine();
                if (requestLine == null) return;

                final var path = requestLine.split(" ")[1];
                final var headers = new JsonObject();
                int contentLength = 0;

                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    final var separator = line.indexOf(':');
                    final var name = line.substring(0, separator).toLowerCase();
                    final var value = line.substring(separator + 1).trim();
                    headers.addProperty(name, value);
                    if (name.equals("content-length")) contentLength = Integer.parseInt(value);
                }

                final var bodyChars = new char[contentLength];
                var read = 0;
                while (read < contentLength) {
                    final var count = reader.read(bodyChars, read, contentLength - read);
                    if (count == -1) break;
                    read += count;
                }

                final var body = new String(bodyChars, 0, read);
                final var e = new Request(path, headers, body.isEmpty() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject());
                System.out.println("parsed body: " + body + ", " + e.body);
                requests.add(e);

                final var response = responses.poll(1, TimeUnit.SECONDS);
                if (response == null) throw new AssertionError("No response enqueued");
                if (response.release() != null) response.release().await(1, TimeUnit.SECONDS);
                writeResponse(client.getOutputStream(), response);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }

        private void writeResponse(final OutputStream output, final Response response) throws IOException {
            final var bytes = response.body().getBytes(StandardCharsets.UTF_8);
            final var headers = "HTTP/1.1 " + response.status() + " OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            output.write(headers.getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
        }

        @Override
        public void close() throws IOException {
            socket.close();
            executor.shutdownNow();
        }
    }

    private record Request(String path, JsonObject headers, JsonObject body) {
    }

    private record Response(int status, String body, @Nullable CountDownLatch release) {
    }
}
