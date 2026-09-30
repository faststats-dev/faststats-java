import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("faststats.root-conventions")
    id("com.gradleup.shadow") version "9.6.1" apply false
    kotlin("jvm") version "2.4.20" apply false
}

// fixme: temp hack to make the project compile
subprojects {
    if (parent?.path !in setOf(":fabric:versions", ":neoforge:versions")) return@subprojects

    pluginManager.apply("com.gradleup.shadow")

    val bundled = configurations.create("bundled") {
        isCanBeConsumed = false
        isTransitive = false
    }
    configurations.named("compileClasspath") { extendsFrom(bundled) }
    tasks.named<ShadowJar>("shadowJar") {
        configurations = listOf(bundled)
        exclude("module-info.class")
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
    }
}
