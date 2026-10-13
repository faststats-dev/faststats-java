import dev.faststats.neoforge.compat.v1_20_6.CompatibilityLayerImpl;
import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.faststats.neoforge.compat.v1_20_6 {
    requires dev.faststats.neoforge;

    requires static org.jspecify;
    requires fml_loader;

    provides dev.faststats.neoforge.compat.CompatibilityLayer with CompatibilityLayerImpl;
}
