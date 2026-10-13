import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("faststats.shadow-conventions")
    id("maven-publish")
}

extra.set("publishArtifactId", "fabric")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/fabric")

tasks.processResources {
    from(project(":fabric").file("src/main/resources"))
    inputs.property("version", provider { project.version })
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

dependencies {
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
