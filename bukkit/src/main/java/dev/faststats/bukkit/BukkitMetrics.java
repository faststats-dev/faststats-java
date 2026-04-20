package dev.faststats.bukkit;

import dev.faststats.Metrics;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface BukkitMetrics extends Metrics permits BukkitMetricsImpl {
    /**
     * Registers additional exception handlers on Paper-based implementations.
     *
     * @throws IllegalPluginAccessException if the plugin is not yet enabled
     * @apiNote This method may only be called {@link Plugin#onEnable() onEnable()}.
     * @since 0.14.0
     */
    @Override
    void ready() throws IllegalPluginAccessException;

    interface Factory extends Metrics.Factory<Factory> {
        @Override
        BukkitMetrics create() throws IllegalStateException;
    }
}
