package dev.faststats;

import com.google.gson.JsonPrimitive;
import dev.faststats.event.Event;
import dev.faststats.event.Key;
import dev.faststats.event.SimpleEventSink;
import org.junit.jupiter.api.Test;

public final class EventSinkTest {
    @Test
    public void testSink() {
        final var sink = new SimpleEventSink();
        sink.queueHeartbeat(Event.of(Key.key("internal", "java_version"), new JsonPrimitive(25)));
        System.out.println(sink.compileAll());
    }
}
