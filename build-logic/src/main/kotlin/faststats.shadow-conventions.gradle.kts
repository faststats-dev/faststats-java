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

fun shadowSources(filter: (ProjectComponentIdentifier) -> Boolean = { true }) = files(shadowClasspath.map { classpath ->
    classpath.incoming.artifactView {
        withVariantReselection()
        componentFilter { it is ProjectComponentIdentifier && filter(it) }
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }.files
})

fun CopySpec.fromSourcesJars(sources: FileCollection) {
    from(sources.elements.map { files -> files.map { zipTree(it) } }) {
        exclude("module-info.java", "META-INF/**")
    }
}

tasks.named<Jar>("sourcesJar") {
    val sources = shadowSources()
    dependsOn(sources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    fromSourcesJars(sources)
}

val javadocSources = tasks.register<Sync>("javadocSources") {
    val sources = shadowSources { !it.projectPath.contains(":versions:") }
    dependsOn(sources)
    // split packages across the shaded projects would otherwise clash, e.g. their package-info.java
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.map { it.allJava }) { exclude("module-info.java") }
    fromSourcesJars(sources)
    include("**/*.java")
    into(layout.buildDirectory.dir("javadocSources"))
}

tasks.named<Javadoc>("javadoc") {
    setSource(javadocSources)
    classpath += shadowClasspath.get()
}

components.named<AdhocComponentWithVariants>("shadow") {
    addVariantsFromConfiguration(configurations["sourcesElements"]) {}
    addVariantsFromConfiguration(configurations["javadocElements"]) {}
}
