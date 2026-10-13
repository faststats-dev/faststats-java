import dev.faststats.fabric.compat.v1_21_9.CompatibilityLayerImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.fabric.compat.v1_21_9 {
    exports dev.faststats.fabric.compat.v1_21_9;

    requires dev.faststats.fabric;

    requires static org.jspecify;

    provides dev.faststats.fabric.compat.CompatibilityLayer with CompatibilityLayerImpl;
}
