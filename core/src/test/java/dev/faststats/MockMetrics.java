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
                .metricsServer(URI.create("http://localhost:5000/v1"))
                .flagsServer(URI.create("http://localhost:5001/v1"))
                .token(token)
                .debug(debug)
                .create(), tracker, null);
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
    }
}
