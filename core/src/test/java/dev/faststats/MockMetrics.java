package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.config.SimpleConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@NullMarked
public final class MockMetrics extends SimpleMetrics {
    public MockMetrics(final UUID serverId, @Token final String token, @Nullable final ErrorTracker tracker, final boolean debug) {
        super(new SimpleConfig(serverId, true, debug, true, true, false), Set.of(), token, tracker, null, URI.create("http://localhost:5000/v1/collect"), debug);
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart();
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
    }
}
