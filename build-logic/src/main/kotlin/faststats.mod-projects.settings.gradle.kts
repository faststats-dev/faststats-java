pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
}

include(
    "fabric",
    "fabric:example-mod",
    "neoforge",
    "neoforge:example-mod",
    "onboarding"
)

fun includeVersionModules(platform: String) {
    file("$platform/versions")
        .listFiles { file -> file.isDirectory && file.resolve("build.gradle.kts").isFile }
        ?.sortedBy { it.name }
        ?.forEach { include("$platform:versions:${it.name}") }
}

includeVersionModules("fabric")
includeVersionModules("neoforge")
includeVersionModules("onboarding")
