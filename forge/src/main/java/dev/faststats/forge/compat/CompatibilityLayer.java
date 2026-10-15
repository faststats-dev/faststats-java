package dev.faststats.forge.compat;

import java.nio.file.Path;
import java.util.Optional;

public interface CompatibilityLayer {
    /**
     * Runs the given tasks once the dedicated server started and when it is stopping.
     */
    void onServerLifecycle(Runnable started, Runnable stopping);

    boolean clientOnlineMode();

    int clientPlayerCount();

    boolean serverOnlineMode();

    int serverPlayerCount();

    Optional<String> modVersion(String modId);

    Path configDirectory();

    Environment getEnvironment();

    enum Environment {
        CLIENT, SERVER
    }
}
