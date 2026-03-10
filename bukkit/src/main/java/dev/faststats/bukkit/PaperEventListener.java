package dev.faststats.bukkit;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerPluginException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

record PaperEventListener(BukkitMetricsImpl metrics) implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerException(final ServerExceptionEvent event) {
        if (!(event.getException() instanceof final ServerPluginException exception)) return;
        if (!exception.getResponsiblePlugin().equals(metrics.plugin())) return;
        final var report = exception.getCause() != null ? exception.getCause() : exception;
        metrics.getErrorTracker().ifPresent(tracker -> tracker.trackError(report));
    }
}
