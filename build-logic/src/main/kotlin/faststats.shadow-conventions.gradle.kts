import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("com.gradleup.shadow")
}

val shadowClasspath = configurations.resolvable("shadowClasspath") {
    extendsFrom(configurations.api.get(), configurations.implementation.get(), configurations.runtimeOnly.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.named<ShadowJar>("shadowJar") {
    configurations.set(shadowClasspath.map { listOf(it) })
    archiveClassifier.set("")
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
}

shadow {
    addTargetJvmVersionAttribute.set(false)
}
configurations.named("shadowRuntimeElements") {
    attributes.attributeProvider(
        TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
        tasks.compileJava.flatMap { it.options.release },
    )
}

components.named<AdhocComponentWithVariants>("shadow") {
    addVariantsFromConfiguration(configurations["sourcesElements"]) {}
    addVariantsFromConfiguration(configurations["javadocElements"]) {}
}
