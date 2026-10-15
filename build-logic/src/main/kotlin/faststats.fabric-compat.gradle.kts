import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("faststats.minecraft-version")
    id("faststats.onboarding-sources")
    id("faststats.shadow-conventions")
    id("maven-publish")
}

val libs = rootProject.the<VersionCatalogsExtension>().named("libs")
val minecraft = minecraftVersion

pluginManager.apply(if (minecraft.obfuscated) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")
val loom = the<LoomGradleExtensionAPI>()

extra.set("publishArtifactId", "fabric")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/fabric")
extra.set("publishVersionSuffix", "mc$minecraft")

tasks.processResources {
    from(project(":fabric").file("src/main/resources"))
    val version = provider { project.version.toString() }
    val minecraftVersion = minecraft.name
    inputs.property("version", version)
    inputs.property("minecraft", minecraftVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to version.get(), "minecraft" to minecraftVersion)
    }
}

dependencies {
    add("minecraft", libs.findLibrary("minecraft-${minecraft.catalogName}").get())
    if (minecraft.obfuscated) add("mappings", loom.officialMojangMappings())

    compileOnly(libs.findLibrary("fabric-api-${minecraft.fabricApiCatalogName}").get())
    compileOnly(libs.findLibrary("fabric-loader").get())
    compileOnly(libs.findLibrary("sponge-mixin").get())

    implementation(project(":core"))
    implementation(project(":config"))
    implementation(project(":onboarding"))
    implementation(project(":fabric", "runtimeElements"))
}

pluginManager.withPlugin("net.fabricmc.fabric-loom-remap") {
    val shadowJar = tasks.named<ShadowJar>("shadowJar") {
        destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
        archiveClassifier.set("all")
    }
    val remapJar = tasks.named<RemapJarTask>("remapJar") {
        inputFile.set(shadowJar.flatMap { it.archiveFile })
    }
    configurations.named("shadowRuntimeElements") {
        outgoing.artifacts.clear()
        outgoing.artifact(remapJar)
    }
}
