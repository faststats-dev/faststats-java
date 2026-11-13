package dev.faststats.fabric.compat;

import net.fabricmc.api.EnvType;

/**
 * Version-specific Fabric integration hooks.
 */
public interface CompatibilityLayer {
    // server exclusive
    @Deprecated
    void initServer();

    // client impl #onlineMode
    @Deprecated
    boolean clientOnlineMode();

    // client impl #playerCount
    @Deprecated
    int clientPlayerCount();

    // server impl #onlineMode
    @Deprecated
    boolean serverOnlineMode();

    // server impl #playerCount
    @Deprecated
    int serverPlayerCount();

    boolean onlineMode();

    int playerCount();

    interface Client extends CompatibilityLayer {

    }

    interface Server extends CompatibilityLayer {

    }

    interface Loader {
        CompatibilityLayer compat();

        EnvType environment();
    }
}
