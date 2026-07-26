plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.0" apply false
    kotlin("jvm") version "2.4.20-Beta2" apply false
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
    }

    group = "dev.faststats.metrics"

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

                from(components["java"])
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

tasks.register("checkPlatformCompat") {
    group = "verification"
    description = "Compiles all platform compatibility modules."
    dependsOn(tasks.named("checkFabricPlatformCompat"), tasks.named("checkNeoForgePlatformCompat"))
}

tasks.register("publishPlatformCompat") {
    group = "publishing"
    description = "Publishes all platform compatibility modules."
    dependsOn(
        platformCompatProjects("fabric").map { "${it.path}:publish" } +
                platformCompatProjects("neoforge").map { "${it.path}:publish" }
    )
}
