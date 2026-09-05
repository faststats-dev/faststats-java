import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1" apply false
    kotlin("jvm") version "2.4.20-RC3" apply false
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
    }

    group = "dev.faststats.metrics"

    if (path.startsWith(":fabric:versions:") || path.startsWith(":neoforge:versions:")) {
        apply { plugin("com.gradleup.shadow") }
        extra.set("publishComponent", "shadow")

        val bundled = configurations.create("bundled") {
            isCanBeConsumed = false
            isTransitive = false
        }
        extensions.configure<ShadowExtension> {
            addShadowVariantIntoJavaComponent = false
        }
        tasks.named<ShadowJar>("shadowJar") {
            configurations = listOf(bundled)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            filesMatching("META-INF/services/**") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
            mergeServiceFiles()
            exclude("module-info.class", "META-INF/versions/**/module-info.class")
            if (project.path.startsWith(":neoforge:")) exclude("fabric.mod.json")
        }
        tasks.named("assemble") { dependsOn("shadowJar") }
        tasks.named<Jar>("jar") {
            enabled = false
        }

        afterEvaluate {
            configurations.named("shadowRuntimeElements") {
                attributes.attributeProvider(
                    TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                    tasks.named<JavaCompile>("compileJava").flatMap { it.options.release }
                )
            }
            val distribution = tasks.named<AbstractArchiveTask>(
                if (plugins.hasPlugin("net.fabricmc.fabric-loom-remap")) "remapJar" else "shadowJar"
            )
            listOf("apiElements", "runtimeElements").forEach { name ->
                configurations.named(name) {
                    outgoing.artifacts.clear()
                    outgoing.variants.clear()
                    outgoing.artifact(distribution)
                    exclude(group = "dev.faststats.metrics")
                }
            }
            configurations.findByName("namedElements")?.outgoing?.apply {
                artifacts.clear()
                variants.clear()
                artifact(tasks.named("shadowJar"))
            }
        }
    }

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    val generateFastStatsProperties = tasks.register("generateFastStatsProperties") {
        description = "Generates the META-INF/faststats.properties file"
        val outputDir = layout.buildDirectory.dir("generated/resources/faststats")
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("META-INF/faststats.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("version=${project.version}\n")
        }
    }

    sourceSets.main { resources.srcDir(generateFastStatsProperties) }

    tasks.test {
        dependsOn(tasks.javadoc)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showCauses = true
            showExceptions = true
        }
    }

    fun ownProperty(name: String): String? {
        return if (extensions.extraProperties.has(name)) extensions.extraProperties.get(name).toString() else null
    }

    tasks.withType<JavaCompile>().configureEach {
        ownProperty("moduleName")?.let { moduleName ->
            options.compilerArgs.addAll(listOf("--add-reads", "$moduleName=ALL-UNNAMED"))
        }
    }

    tasks.withType<Test>().configureEach {
        ownProperty("moduleName")?.let { moduleName ->
            jvmArgs("--add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    tasks.withType<JavaExec>().configureEach {
        ownProperty("moduleName")?.let { moduleName ->
            jvmArgs("--add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    tasks.javadoc {
        val options = options as StandardJavadocDocletOptions
        options.tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
        ownProperty("moduleName")?.let { moduleName ->
            options.addStringOption("-add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    afterEvaluate {
        val publishArtifactId = ownProperty("publishArtifactId")
        if (!plugins.hasPlugin("maven-publish") && publishArtifactId == null) return@afterEvaluate
        if (!plugins.hasPlugin("maven-publish") || publishArtifactId == null) throw IllegalStateException(
            "Invalid publishing setup for project \"${project.path}\", " +
                    "maven-publish: ${plugins.hasPlugin("maven-publish")}, publishArtifactId: $publishArtifactId"
        )

        ownProperty("publishVersionSuffix")?.let { suffix ->
            version = "${rootProject.version}+$suffix"
        }

        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                artifactId = publishArtifactId
                groupId = "dev.faststats.metrics"

                pom {
                    url.set(
                        ownProperty("publishDocsUrl")
                            ?: throw IllegalStateException("No docs URL provided by \"${project.path}\"")
                    )
                    scm {
                        val repository = "faststats-dev/faststats-java"
                        url.set("https://github.com/$repository")
                        connection.set("scm:git:git://github.com/$repository.git")
                        developerConnection.set("scm:git:ssh://github.com/$repository.git")
                    }
                }

                from(components[ownProperty("publishComponent") ?: "java"])
                if (ownProperty("publishComponent") == "shadow") {
                    artifact(tasks.named(if (plugins.hasPlugin("net.fabricmc.fabric-loom-remap")) "remapSourcesJar" else "sourcesJar"))
                    artifact(tasks.named("javadocJar"))
                }
            }

            repositories {
                maven {
                    val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
                    url = uri("https://repo.faststats.dev/$channel")
                    credentials {
                        username = System.getenv("REPOSITORY_USER")
                        password = System.getenv("REPOSITORY_TOKEN")
                    }
                }
            }
        }
    }
}

fun platformCompatProjects(platform: String) = subprojects.filter { project ->
    project.path.startsWith(":$platform:versions:")
}

tasks.register("checkFabricPlatformCompat") {
    group = "verification"
    description = "Compiles all Fabric platform compatibility modules."
    dependsOn(platformCompatProjects("fabric").map { "${it.path}:compileJava" })
}

tasks.register("checkNeoForgePlatformCompat") {
    group = "verification"
    description = "Compiles all NeoForge platform compatibility modules."
    dependsOn(platformCompatProjects("neoforge").map { "${it.path}:compileJava" })
}

tasks.register("checkOnboardingCompat") {
    group = "verification"
    description = "Compiles every onboarding band against its own Minecraft version."
    dependsOn(platformCompatProjects("onboarding").map { "${it.path}:compileJava" })
}

tasks.register("checkPlatformCompat") {
    group = "verification"
    description = "Compiles all platform compatibility modules."
    dependsOn("checkFabricPlatformCompat", "checkNeoForgePlatformCompat", "checkOnboardingCompat")
}

tasks.register("assemblePlatformCompat") {
    group = "build"
    description = "Assembles all Fabric and NeoForge distributions, including onboarding."
    dependsOn(
        (platformCompatProjects("fabric") + platformCompatProjects("neoforge"))
            .map { "${it.path}:assemble" }
    )
}

tasks.register("publishPlatformCompat") {
    group = "publishing"
    description = "Publishes all platform compatibility modules."
    dependsOn(
        platformCompatProjects("fabric").map { "${it.path}:publish" } +
                platformCompatProjects("neoforge").map { "${it.path}:publish" }
    )
}
