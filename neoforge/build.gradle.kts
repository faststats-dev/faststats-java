import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

extra.set("moduleName", "dev.faststats.neoforge")

plugins {
    id("net.neoforged.moddev") version "2.0.144"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

configurations.runtimeClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

tasks.processResources {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

allprojects {
    if (project.name == "example-mod") return@allprojects
    if (project.path == ":neoforge:versions") return@allprojects
    apply { plugin("maven-publish") }
    extra.set("publishArtifactId", "neoforge")
    extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/neoforge")
}

subprojects {
    if (parent?.path != ":neoforge:versions") return@subprojects

    tasks.processResources {
        from(project(":neoforge").file("src/main/resources"))
        inputs.property("version", provider { project.version })
        filesMatching("META-INF/neoforge.mods.toml") {
            expand("version" to project.version)
        }
    }

    tasks.named<ShadowJar>("shadowJar") { archiveClassifier.set("") }
    tasks.jar { archiveClassifier.set("thin") }

    apply { plugin("net.neoforged.moddev") }

    dependencies {
        compileOnly("net.neoforged:bus:8.0.5")
        compileOnlyApi(project(":neoforge"))
        compileOnlyApi(project(":onboarding"))
        "bundled"(project(":core"))
        "bundled"(project(":config"))
        "bundled"(project(":onboarding"))
        "bundled"(project(":neoforge"))
    }
}

neoForge {
    version = "21.8.53" // lowest bound, 1.20.6
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    compileOnlyApi(project(":onboarding"))
    compileOnly("net.neoforged:bus:8.0.5")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
}
