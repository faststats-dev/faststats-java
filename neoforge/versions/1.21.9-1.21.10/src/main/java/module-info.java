import dev.faststats.neoforge.compat.v1_21_9.CompatibilityLayerImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.neoforge.compat.v1_21_9 {
    requires dev.faststats.neoforge;

    requires static org.jspecify;
    requires fml_loader;

    provides dev.faststats.neoforge.compat.CompatibilityLayer with CompatibilityLayerImpl;
}
