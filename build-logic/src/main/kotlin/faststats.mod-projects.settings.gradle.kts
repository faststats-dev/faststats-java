// -PmodPlatforms=fabric,forge limits which mod platforms are included, -PmodPlatforms=none excludes all of them
val modPlatforms = providers.gradleProperty("modPlatforms").orNull?.let { property ->
    property.split(',').map(String::trim).filter { it.isNotEmpty() && it != "none" }.toSet().onEach {
        require(it in minecraftPlatforms) { "Unknown mod platform \"$it\", expected one of $minecraftPlatforms" }
    }
} ?: minecraftPlatforms

include("onboarding")
modPlatforms.forEach { include(it) }

(minecraftPlatforms + "onboarding").forEach { platform ->
    file("$platform/versions").listFiles { file -> file.resolve("src").isDirectory }?.forEach { variant ->
        require(minecraftVersions(platform).any { it.name == variant.name }) {
            "$platform/versions/${variant.name} is not a supported Minecraft version for $platform"
        }
    }
}

// one project per platform and Minecraft version, see MinecraftVersions.kt
modPlatforms.forEach { platform ->
    minecraftVersions(platform).forEach { version ->
        val path = ":$platform:versions:${version.name}"
        include(path)
        project(path).projectDir = file("$platform/versions/${version.name}").apply { mkdirs() }
    }
}
