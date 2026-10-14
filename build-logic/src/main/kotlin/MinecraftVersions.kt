import org.gradle.api.Project
import java.io.File

/**
 * A Minecraft version the mod platforms are compiled and published for.
 *
 * @param java the Java version the game runs on
 * @param obfuscated whether the game is obfuscated and the jars have to be remapped
 * @param fabricApi the Fabric API release to compile against
 */
class MinecraftVersion(
    val name: String,
    val java: Int,
    val obfuscated: Boolean = true,
    fabricApi: String = name,
) {
    // the version catalog names its entries like minecraft-v1x21x10
    val catalogName = catalogName(name)
    val fabricApiCatalogName = catalogName(fabricApi)

    override fun toString() = name
}

private fun catalogName(version: String) = "v" + version.replace('.', 'x')

// every supported Minecraft version, oldest first
// a version without its own directory in <platform>/versions is built from the closest older one with sources
val minecraftVersions = listOf(
    MinecraftVersion("1.16.1", java = 17),
    MinecraftVersion("1.16.2", java = 17),
    MinecraftVersion("1.16.3", java = 17),
    MinecraftVersion("1.16.4", java = 17),
    MinecraftVersion("1.16.5", java = 17),
    MinecraftVersion("1.17", java = 17),
    MinecraftVersion("1.17.1", java = 17),
    MinecraftVersion("1.18", java = 17),
    MinecraftVersion("1.18.1", java = 17),
    MinecraftVersion("1.18.2", java = 17),
    MinecraftVersion("1.19", java = 17),
    MinecraftVersion("1.19.1", java = 17),
    MinecraftVersion("1.19.2", java = 17),
    MinecraftVersion("1.19.3", java = 17),
    MinecraftVersion("1.19.4", java = 17),
    MinecraftVersion("1.20", java = 17),
    MinecraftVersion("1.20.1", java = 17),
    MinecraftVersion("1.20.2", java = 17),
    MinecraftVersion("1.20.3", java = 17),
    MinecraftVersion("1.20.4", java = 17),
    MinecraftVersion("1.20.5", java = 21),
    MinecraftVersion("1.20.6", java = 21),
    MinecraftVersion("1.21", java = 21),
    MinecraftVersion("1.21.1", java = 21),
    MinecraftVersion("1.21.2", java = 21),
    MinecraftVersion("1.21.3", java = 21),
    MinecraftVersion("1.21.4", java = 21),
    MinecraftVersion("1.21.5", java = 21),
    MinecraftVersion("1.21.6", java = 21),
    MinecraftVersion("1.21.7", java = 21),
    MinecraftVersion("1.21.8", java = 21),
    MinecraftVersion("1.21.9", java = 21),
    MinecraftVersion("1.21.10", java = 21),
    MinecraftVersion("1.21.11", java = 21),
    MinecraftVersion("26.1", java = 25, obfuscated = false, fabricApi = "26.1.2"),
    MinecraftVersion("26.1.1", java = 25, obfuscated = false, fabricApi = "26.1.2"),
    MinecraftVersion("26.1.2", java = 25, obfuscated = false),
    MinecraftVersion("26.2", java = 25, obfuscated = false),
    MinecraftVersion("26.3", java = 25, obfuscated = false),
)

private val platformVersions = mapOf(
    "fabric" to minecraftVersions,
    "neoforge" to minecraftVersions.dropWhile { it.name != "1.20.6" },
    "onboarding" to minecraftVersions,
)

// the platforms with a project per Minecraft version, the onboarding sources are compiled into them
val minecraftPlatforms = setOf("fabric", "neoforge")

fun minecraftVersions(platform: String): List<MinecraftVersion> = platformVersions.getValue(platform)

// projects are named :<platform>:versions:<minecraft version>
val Project.minecraftPlatform: String get() = path.split(':')[1]

val Project.minecraftVersion: MinecraftVersion
    get() = minecraftVersions(minecraftPlatform).single { it.name == name }

/**
 * The version whose sources [version] is built from, the closest version up to it with its own directory.
 */
fun Project.minecraftVariant(
    platform: String = minecraftPlatform,
    version: MinecraftVersion = minecraftVersion,
): MinecraftVersion = minecraftVersions(platform)
    .takeWhile { it.name != version.name }
    .plus(version)
    .last { variantDirectory(platform, it).resolve("src").isDirectory }

fun Project.variantDirectory(
    platform: String = minecraftPlatform,
    variant: MinecraftVersion = minecraftVariant(platform),
): File = rootProject.file("$platform/versions/${variant.name}")
