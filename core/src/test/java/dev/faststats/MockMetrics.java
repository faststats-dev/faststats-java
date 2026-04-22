package dev.faststats;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;

import java.net.URI;

@NullMarked
final class MockMetrics extends SimpleMetrics {
    MockMetrics(final Factory factory) {
        super(factory, URI.create("http://localhost:5000/v1/collect"));
    }

    @Override
    protected boolean preSubmissionStart() {
        return true;
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
    }
}
