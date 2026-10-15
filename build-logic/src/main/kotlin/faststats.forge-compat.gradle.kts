import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject

plugins {
    id("net.minecraftforge.gradle")
    id("faststats.minecraft-version")
    id("faststats.onboarding-sources")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

val libs = rootProject.the<VersionCatalogsExtension>().named("libs")
val minecraft = minecraftVersion

extra.set("publishArtifactId", "forge")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/forge")
extra.set("publishVersionSuffix", "mc$minecraft")

val forgeGradle = the<ForgeGradleExtension>()
val minecraftExtension = the<MinecraftExtensionForProject>()

if (minecraft.obfuscated) minecraftExtension.mappings("official", minecraft.name)

repositories {
    minecraftExtension.mavenizer(this)
    maven(forgeGradle.forgeMaven)
    maven(forgeGradle.minecraftLibsMaven)
}

sourceSets.main {
    java.srcDir(project(":forge").file("src/mod/java"))
}

tasks.processResources {
    from(project(":forge").file("src/main/resources"))
    val version = provider { project.version.toString() }
    val range = "[${minecraft.name}]"
    inputs.property("version", version)
    inputs.property("minecraft", range)
    filesMatching("META-INF/mods.toml") {
        expand("version" to version.get(), "minecraft" to range)
    }
    // Forge ships Mixin 0.8.5 (1.20.6) or 0.8.7, which don't know compatibility levels above JAVA_18 or JAVA_21
    val maxMixinLevel = if (minecraft.name == "1.20.6") 18 else 21
    inputs.property("maxMixinLevel", maxMixinLevel)
    filesMatching("faststats-onboarding.mixins.json") {
        filter { line ->
            line.replace(Regex("\"JAVA_(\\d+)\"")) { level ->
                if (level.groupValues[1].toInt() > maxMixinLevel) "\"JAVA_$maxMixinLevel\"" else level.value
            }
        }
    }
}

tasks.withType<Jar>().configureEach {
    manifest.attributes("MixinConfigs" to "faststats-onboarding.mixins.json")
}

val forge = libs.findVersion("forge-${minecraft.catalogName}").get().requiredVersion

dependencies {
    compileOnly(minecraftExtension.dependency("net.minecraftforge:forge:$forge"))
    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":onboarding"))
    implementation(project(":forge"))
}
