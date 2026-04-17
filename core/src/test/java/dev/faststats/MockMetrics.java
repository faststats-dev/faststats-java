package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.Settings;
import dev.faststats.core.SimpleMetrics;
import dev.faststats.core.Token;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@NullMarked
public final class MockMetrics extends SimpleMetrics {
    public MockMetrics(final UUID serverId, @Token final String token, @Nullable final ErrorTracker tracker, final boolean debug) {
        super(new Config(serverId, true, debug, true, true, false, false), Set.of(), Settings.factory()
                .url(URI.create("http://localhost:5000/v1/collect"))
                .token(token)
                .debug(debug)
                .create(), tracker, null);
    }

    @Override
    protected void printError(final String message, @Nullable final Throwable throwable) {
        System.err.println(message);
        if (throwable != null) throwable.printStackTrace(System.err);
    }

    @Override
    protected void printInfo(final String message) {
        System.out.println(message);
    }

    @Override
    protected void printWarning(final String message) {
        System.out.println(message);
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
    }
}
