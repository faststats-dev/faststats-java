package dev.faststats.quilt;

import com.google.gson.JsonObject;
import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

final class QuiltMetrics extends SimpleMetrics {
    private final ModContainer mod;

    private QuiltMetrics(final Factory factory, final ModContainer mod) {
        super(factory);
        this.mod = mod;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        // todo: implement
        //  metrics.addProperty("online_mode", false); 
        //  metrics.addProperty("player_count", 0);
        //  metrics.addProperty("platform_version", "idk");
        metrics.addProperty("minecraft_version", QuiltLoader.getNormalizedGameVersion());
        metrics.addProperty("plugin_version", mod.metadata().version().raw());
        metrics.addProperty("server_type", "Quilt");
    }

    static final class Factory extends SimpleMetrics.Factory {
        private final ModContainer mod;

        Factory(final SimpleContext context, final ModContainer mod) {
            super(context);
            this.mod = mod;
        }

        @Override
        public Metrics create() throws IllegalStateException {
            return new QuiltMetrics(this, mod);
        }
    }
}
