package dev.faststats.forge.compat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Version-specific Forge integration hooks.
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

        public abstract void whenStopping(Runnable task);

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

        Environment environment();

        Optional<String> modVersion(String modId);

        Path configDirectory();
    }

    enum Environment {
        CLIENT, SERVER
    }
}
