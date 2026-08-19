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
    if (project.name == "example-mod") return@subprojects

    // todo: move to respective sub-module
    val onboardingBand = when (project.name) {
        "1.16.1-1.17.1" -> ":onboarding:versions:1.16.1-1.17.1"
        "1.18-1.18.2" -> ":onboarding:versions:1.18-1.18.2"
        "1.19-1.19.3" -> ":onboarding:versions:1.19-1.19.3"
        "1.18-1.21.8" -> ":onboarding:versions:1.19.4-1.21.8"
        "1.21.9-1.21.11" -> ":onboarding:versions:1.21.9-1.21.11"
        "26.1-26.3" -> ":onboarding:versions:26.1-26.3"
        else -> null
    }
    evaluationDependsOn(":onboarding")
    onboardingBand?.let { evaluationDependsOn(it) }

    dependencies {
        compileOnlyApi(project(":fabric"))
        compileOnlyApi(project(":onboarding"))
        onboardingBand?.let {
            compileOnlyApi(project(it))
        }
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":fabric").sourceSets["main"].output)
        from(project(":config").sourceSets["main"].output)
        from(project(":core").sourceSets["main"].output)
        from(project(":onboarding").sourceSets["main"].output)
        onboardingBand?.let { from(project(it).sourceSets["main"].output) }
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
