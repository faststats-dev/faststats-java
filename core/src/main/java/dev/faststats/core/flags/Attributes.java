package dev.faststats.core.flags;

import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Mutable key-value attributes for feature flag targeting.
 * <p>
 * Attributes are sent to the server on each flag fetch
 * so that targeting rules can be evaluated server-side.
 *
 * @since 0.23.0
 */
public sealed interface Attributes permits SimpleAttributes {
    /**
     * Create new empty attributes.
     *
     * @return new attributes
     * @since 0.23.0
     */
    @Contract(value = " -> new", pure = true)
    static Attributes create() {
        return new SimpleAttributes(new ConcurrentHashMap<>());
    }

    /**
     * Create new attributes by copying entries from the given source.
     *
     * @param attributes the source attributes to copy
     * @return new attributes containing the copied entries
     * @since 0.23.0
     */
    @Contract(value = "_ -> new", pure = true)
    static Attributes copyOf(final Attributes attributes) {
        final var entries = ((SimpleAttributes) attributes).attributes();
        return new SimpleAttributes(new ConcurrentHashMap<>(entries));
    }

    /**
     * Set a string value.
     *
     * @param key   the key
     * @param value the value
     * @return these attributes
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    Attributes put(String key, String value);

    /**
     * Set a number value.
     *
     * @param key   the key
     * @param value the value
     * @return these attributes
     * @throws IllegalArgumentException if the given value is not {@link Double#isFinite(double) finite}
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    Attributes put(String key, Number value) throws IllegalArgumentException;

    /**
     * Set a boolean value.
     *
     * @param key   the key
     * @param value the value
     * @return these attributes
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    Attributes put(String key, boolean value);

    /**
     * Remove a value.
     *
     * @param key the key
     * @return these attributes
     * @since 0.23.0
     */
    @Contract(value = "_ -> this", mutates = "this")
    Attributes remove(String key);

    /**
     * Visit each stored attribute as its underlying JSON primitive value.
     *
     * @param action the action to invoke for each key-value pair
     * @since 0.23.0
     */
    void forEachPrimitive(BiConsumer<String, JsonPrimitive> action);

    /**
     * Create new attributes by merging two attribute sets.
     * <p>
     * If both contain the same key, the value from {@code second} takes precedence.
     *
     * @param first  the first attributes
     * @param second the second attributes, takes precedence on conflicts
     * @return new merged attributes
     * @since 0.23.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Attributes join(@Nullable final Attributes first, @Nullable final Attributes second) {
        final var attributes = new ConcurrentHashMap<String, JsonPrimitive>();
        if (first instanceof final SimpleAttributes simple) attributes.putAll(simple.attributes());
        if (second instanceof final SimpleAttributes simple) attributes.putAll(simple.attributes());
        return new SimpleAttributes(attributes);
    }
}
