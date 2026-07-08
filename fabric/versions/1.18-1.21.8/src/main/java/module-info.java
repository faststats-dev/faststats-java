import dev.faststats.fabric.compat.v1_18.CompatibilityLayerImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.fabric.compat.v1_18 {
    exports dev.faststats.fabric.compat.v1_18;

    requires dev.faststats.fabric;

    requires static org.jspecify;

    provides dev.faststats.fabric.compat.CompatibilityLayer with CompatibilityLayerImpl;
}
