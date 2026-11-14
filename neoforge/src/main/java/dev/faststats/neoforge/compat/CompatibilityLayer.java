package dev.faststats.neoforge.compat;

import net.neoforged.api.distmarker.Dist;

import java.util.ArrayList;
import java.util.List;

/**
 * Version-specific NeoForge integration hooks.
 */
public interface CompatibilityLayer {
    boolean onlineMode();

    int playerCount();

    interface Client extends CompatibilityLayer {
    }

    abstract class Server implements CompatibilityLayer {
        private final List<Runnable> pending = new ArrayList<>();
        private boolean started;

        public abstract void initServer();

        public final void whenReady(final Runnable task) {
            synchronized (pending) {
                if (!started) {
                    pending.add(task);
                    return;
                }
            }
            task.run();
        }

        protected final void onStarted() {
            final List<Runnable> tasks;
            synchronized (pending) {
                started = true;
                tasks = List.copyOf(pending);
                pending.clear();
            }
            tasks.forEach(Runnable::run);
        }
    }

    interface Loader {
        CompatibilityLayer compat();

        Dist environment();
    }
}
