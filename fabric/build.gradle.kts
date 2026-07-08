extra.set("moduleName", "dev.faststats.fabric")

plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
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

    val fabricJar = project(":fabric").tasks.named<Jar>("jar")

    dependencies {
        compileOnlyApi(project(":fabric"))
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(fabricJar)
        from(fabricJar.flatMap { it.archiveFile }.map { it.asFile })
    }
}

dependencies {
    compileOnlyApi(project(":core"))
    compileOnly(project(":config"))
    minecraft("com.mojang:minecraft:26.1.2")
    compileOnly("net.fabricmc.fabric-api:fabric-api:0.150.0+26.1.2")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
}
