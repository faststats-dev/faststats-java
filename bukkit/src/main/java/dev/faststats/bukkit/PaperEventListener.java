package dev.faststats.bukkit;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerPluginException;
import dev.faststats.FastStatsContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

record PaperEventListener(Plugin plugin, FastStatsContext context) implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerException(final ServerExceptionEvent event) {
        if (!(event.getException() instanceof final ServerPluginException exception)) return;
        if (!exception.getResponsiblePlugin().equals(plugin)) return;
        final var report = exception.getCause() != null ? exception.getCause() : exception;
        context.errorTrackerService().ifPresent(service -> {
            service.globalErrorTracker().trackError(report).handled(false);
        });
    }
}
