plugins {
    id("java")
}

subprojects {
    pluginManager.apply("faststats.java-conventions")
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
