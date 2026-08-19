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
    if (project.name == "example-mod") return@subprojects

    apply { plugin("net.neoforged.moddev") }

    val onboardingBand = when (project.name) {
        "1.20.6-1.21.8" -> ":onboarding:versions:1.19.4-1.21.8"
        "1.21.9-1.21.11" -> ":onboarding:versions:1.21.9-1.21.11"
        "26.1-26.2" -> ":onboarding:versions:26.1-26.3"
        else -> null
    }
    evaluationDependsOn(":onboarding")
    onboardingBand?.let { evaluationDependsOn(it) }

    dependencies {
        compileOnly("net.neoforged:bus:8.0.5")
        compileOnlyApi(project(":neoforge"))
        compileOnlyApi(project(":onboarding"))
        onboardingBand?.let {
            compileOnlyApi(project(it))
        }
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":neoforge").sourceSets["main"].output)
        from(project(":config").sourceSets["main"].output)
        from(project(":core").sourceSets["main"].output)
        from(project(":onboarding").sourceSets["main"].output)
        onboardingBand?.let { from(project(it).sourceSets["main"].output) }
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
