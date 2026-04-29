plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1" apply false
}

val javaVersionsOverride = mapOf(
    ":bukkit" to 21,
    ":bukkit:example-plugin" to 21,
    ":fabric" to 21,
    ":fabric:example-mod" to 21,
    ":hytale" to 25,
    ":hytale:example-plugin" to 25,
    ":minestom" to 25,
    ":velocity" to 21,
    ":velocity:example-plugin" to 21
)
val defaultJavaVersion = 17

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
    }

    val noPublish = project.name.startsWith("example") || project.name == "config"
    if (noPublish) {
        apply { plugin("com.gradleup.shadow") }
    } else {
        apply { plugin("maven-publish") }
    }

    group = "dev.faststats.metrics"

    repositories {
        mavenCentral()
    }

    val javaVersion = javaVersionsOverride[project.path] ?: defaultJavaVersion

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
        withSourcesJar()
        withJavadocJar()
    }

    tasks.compileJava {
        options.release.set(javaVersion)
    }

    val generateFastStatsProperties by tasks.registering {
        val outputDir = layout.buildDirectory.dir("generated/resources/faststats")
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("META-INF/faststats.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("name=${project.name}\nversion=${project.version}\n")
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

    tasks.withType<JavaCompile>().configureEach {
        project.findProperty("moduleName")?.let { moduleName ->
            options.compilerArgs.addAll(listOf("--add-reads", "$moduleName=ALL-UNNAMED"))
        }
    }

    tasks.withType<Test>().configureEach {
        project.findProperty("moduleName")?.let { moduleName ->
            jvmArgs("--add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    tasks.withType<JavaExec>().configureEach {
        project.findProperty("moduleName")?.let { moduleName ->
            jvmArgs("--add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    tasks.javadoc {
        val options = options as StandardJavadocDocletOptions
        options.tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:")
        project.findProperty("moduleName")?.let { moduleName ->
            options.addStringOption("-add-reads", "$moduleName=ALL-UNNAMED")
        }
    }

    afterEvaluate {
        if (noPublish) return@afterEvaluate
        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                artifactId = project.name
                groupId = "dev.faststats.metrics"

                pom {
                    url.set("https://faststats.dev/docs")
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
