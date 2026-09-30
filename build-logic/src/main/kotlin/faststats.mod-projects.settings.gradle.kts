include(
    "fabric",
    "neoforge",
)

fun includeVersionModules(platform: String) {
    file("$platform/versions")
        .listFiles { file -> file.isDirectory && file.resolve("build.gradle.kts").isFile }
        ?.sortedBy { it.name }
        ?.forEach { include("$platform:versions:${it.name}") }
}

includeVersionModules("fabric")
includeVersionModules("neoforge")
