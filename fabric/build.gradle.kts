import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

extra.set("moduleName", "dev.faststats.fabric")

plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

allprojects {
    if (project.name == "example-mod") return@allprojects
    if (project.path == ":fabric:versions") return@allprojects
    apply { plugin("maven-publish") }
    extra.set("publishArtifactId", "fabric")
    extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/fabric")
}

subprojects {
    if (parent?.path != ":fabric:versions") return@subprojects

    afterEvaluate {
        if (plugins.hasPlugin("net.fabricmc.fabric-loom-remap")) {
            tasks.named<ShadowJar>("shadowJar") {
                destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
            }
            tasks.named<RemapJarTask>("remapJar") {
                inputFile.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
            }
            configurations.named("shadowRuntimeElements") {
                outgoing.artifacts.clear()
                outgoing.artifact(tasks.named("remapJar"))
            }
        } else {
            tasks.named<ShadowJar>("shadowJar") { archiveClassifier.set("") }
        }
    }

    tasks.processResources {
        from(project(":fabric").file("src/main/resources"))
        inputs.property("version", provider { project.version })
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    dependencies {
        compileOnlyApi(project(":fabric"))
        compileOnlyApi(project(":onboarding"))
        "bundled"(project(":core"))
        "bundled"(project(":config"))
        "bundled"(project(":onboarding"))
        "bundled"(project(mapOf("path" to ":fabric", "configuration" to "runtimeElements")))
    }
}

dependencies {
    compileOnlyApi(project(":core"))
    compileOnly(project(":config"))
    compileOnly(project(":onboarding"))
    minecraft("com.mojang:minecraft:26.1.2")
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.150.0+26.1.2")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}
