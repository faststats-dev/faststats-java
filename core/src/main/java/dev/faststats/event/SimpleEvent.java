package dev.faststats.event;

import com.google.gson.JsonElement;

record SimpleEvent(Key key, JsonElement payload) implements Event {
}
