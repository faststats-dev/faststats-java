include(
    "fabric",
    "neoforge",
    "onboarding"
)

(minecraftPlatforms + "onboarding").forEach { platform ->
    file("$platform/versions").listFiles { file -> file.resolve("src").isDirectory }?.forEach { variant ->
        require(minecraftVersions(platform).any { it.name == variant.name }) {
            "$platform/versions/${variant.name} is not a supported Minecraft version for $platform"
        }
    }
}

// one project per platform and Minecraft version, see MinecraftVersions.kt
minecraftPlatforms.forEach { platform ->
    minecraftVersions(platform).forEach { version ->
        val path = ":$platform:versions:${version.name}"
        include(path)
        project(path).projectDir = file("$platform/versions/${version.name}").apply { mkdirs() }
    }
}
