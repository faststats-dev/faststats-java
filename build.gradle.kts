import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar

plugins {
    id("java")
    id("xyz.wagyourtail.jvmdowngrader") version "1.3.6" apply false
    id("com.gradleup.shadow") version "9.3.2" apply false
}

val downgradedVersions = mapOf<String, Set<Int>>(
//    "bukkit" to setOf(8, 11, 16),
//    "bungeecord" to setOf(8, 11, 16),
//    "core" to setOf(8, 11, 16),
//    "velocity" to setOf(17)
)
val javaVersionsOverride = mapOf(
    "minestom" to 25
)
val defaultJavaVersion = 21

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    val example = project.name.startsWith("example")
    if (example) {
        apply(plugin = "com.gradleup.shadow")
    } else {
        apply(plugin = "maven-publish")
    }

    if (downgradedVersions.containsKey(project.name)) {
        apply(plugin = "xyz.wagyourtail.jvmdowngrader")
    }

    group = "dev.faststats.metrics"

    repositories {
        mavenCentral()
    }

    val javaVersion = javaVersionsOverride[project.name] ?: defaultJavaVersion

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
        withSourcesJar()
        withJavadocJar()
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

    tasks.compileJava {
        options.release.set(javaVersion)
    }

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

    tasks.build {
        if (!downgradedVersions.containsKey(project.name)) return@build
        dependsOn(tasks.named("shadeDowngradedApi"))
    }

    downgradedVersions[project.name]?.forEach { javaVersion ->
        // Create downgradeJar task
        tasks.register<DowngradeJar>("downgradeJar$javaVersion") {
            group = "jvmdowngrader"
            description = "Downgrade JAR to Java $javaVersion compatibility"

            dependsOn(tasks.jar)

            inputFile.set(tasks.jar.flatMap { it.archiveFile })
            downgradeTo.set(JavaVersion.toVersion(javaVersion))
            classpath = (sourceSets["main"].compileClasspath)
            archiveClassifier.set("java-$javaVersion")
        }

        // Create downgradeSources task
        tasks.register<DowngradeJar>("downgradeSources$javaVersion") {
            group = "jvmdowngrader"
            description = "Downgrade sources JAR to Java $javaVersion compatibility"

            dependsOn(tasks.named("sourcesJar"))

            inputFile.set(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
            downgradeTo.set(JavaVersion.toVersion(javaVersion))
            classpath = (sourceSets["main"].compileClasspath)
            archiveClassifier.set("java-$javaVersion-sources")
        }

        // Create downgradeJavadoc task
        tasks.register<DowngradeJar>("downgradeJavadoc$javaVersion") {
            group = "jvmdowngrader"
            description = "Downgrade javadoc JAR to Java $javaVersion compatibility"

            dependsOn(tasks.named("javadocJar"))

            inputFile.set(tasks.named<Jar>("javadocJar").flatMap { it.archiveFile })
            downgradeTo.set(JavaVersion.toVersion(javaVersion))
            classpath = (sourceSets["main"].compileClasspath)
            archiveClassifier.set("java-$javaVersion-javadoc")
        }
    }

    afterEvaluate {
        if (example) return@afterEvaluate
        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                artifactId = project.name
                groupId = "dev.faststats.metrics"

                pom {
                    url.set("https://faststats.dev/docs")
                    scm {
                        val repository = "faststats-dev/dev-kits"
                        url.set("https://github.com/$repository")
                        connection.set("scm:git:git://github.com/$repository.git")
                        developerConnection.set("scm:git:ssh://github.com/$repository.git")
                    }
                }

                from(components["java"])
            }

            // Create publications for downgraded versions
            downgradedVersions[project.name]?.forEach { javaVersion ->
                publications.create<MavenPublication>("mavenJava${javaVersion}") {
                    artifactId = project.name
                    groupId = "dev.faststats.metrics.java-$javaVersion"

                    pom {
                        url.set("https://faststats.dev/docs")
                        scm {
                            val repository = "faststats-dev/dev-kits"
                            url.set("https://github.com/$repository")
                            connection.set("scm:git:git://github.com/$repository.git")
                            developerConnection.set("scm:git:ssh://github.com/$repository.git")
                        }
                        description.set("Downgraded to Java $javaVersion compatibility")
                    }

                    // Add downgraded jar
                    artifact(tasks.named("downgradeJar$javaVersion"))

                    // Add downgraded sources if available
                    tasks.findByName("downgradeSources$javaVersion")?.let { sourcesTask ->
                        artifact(sourcesTask) {
                            classifier = "sources"
                        }
                    }

                    // Add downgraded javadoc if available
                    tasks.findByName("downgradeJavadoc$javaVersion")?.let { javadocTask ->
                        artifact(javadocTask) {
                            classifier = "javadoc"
                        }
                    }
                }
            }

            repositories {
                maven {
                    val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
                    url = uri("https://repo.thenextlvl.net/$channel")
                    credentials {
                        username = System.getenv("REPOSITORY_USER")
                        password = System.getenv("REPOSITORY_TOKEN")
                    }
                }
            }
        }
    }
}
