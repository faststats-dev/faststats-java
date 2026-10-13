import dev.faststats.fabric.compat.v1_19_4.CompatibilityLayerImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.fabric.compat.v1_19_4 {
    exports dev.faststats.fabric.compat.v1_19_4;

    requires dev.faststats.fabric;

    requires static org.jspecify;

    provides dev.faststats.fabric.compat.CompatibilityLayer with CompatibilityLayerImpl;
}
