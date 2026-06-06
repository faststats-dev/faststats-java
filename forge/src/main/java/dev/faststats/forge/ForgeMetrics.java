package dev.faststats.forge;

import com.google.gson.JsonObject;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import net.minecraftforge.forgespi.language.IModInfo;

final class ForgeMetrics extends SimpleMetrics {
    private final IModInfo mod;

    private ForgeMetrics(final Factory factory, final IModInfo mod) {
        super(factory);
        this.mod = mod;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        // todo: implement
        //  metrics.addProperty("online_mode", false); 
        //  metrics.addProperty("player_count", 0);
        //  metrics.addProperty("minecraft_version", "idk");
        //  metrics.addProperty("platform_version", "idk");
        metrics.addProperty("plugin_version", mod.getVersion().toString());
        metrics.addProperty("server_type", "Forge");
    }

    static final class Factory extends SimpleMetrics.Factory {
        private final IModInfo mod;

        Factory(final SimpleContext context, final IModInfo mod) {
            super(context);
            this.mod = mod;
        }

        @Override
        public Metrics create() throws IllegalStateException {
            return new ForgeMetrics(this, mod);
        }
    }
}
