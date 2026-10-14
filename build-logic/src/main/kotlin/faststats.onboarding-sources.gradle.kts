// compiles the onboarding sources for the project's Minecraft version into the platform jar
plugins {
    id("java")
}

val variant = minecraftVariant("onboarding")
val directory = variantDirectory("onboarding", variant)

// files that are identical between variants only exist in the oldest variant using them
class SharedSource(val owner: String, val path: String, vararg val users: String)

val sharedSources = listOf(
    SharedSource(
        "1.16.1", "java/dev/faststats/screen/minecraft/ConsentScreen.java",
        "1.16.2"
    ),
    SharedSource(
        "1.16.1", "java/dev/faststats/screen/minecraft/mixin/OptionsScreenMixin.java",
        "1.16.2"
    ),
    SharedSource(
        "1.16.1", "java/dev/faststats/screen/minecraft/mixin/TitleScreenMixin.java",
        "1.16.2", "1.17", "1.19", "1.19.3", "1.19.4", "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21.4", "1.21.9"
    ),
    SharedSource(
        "1.16.1", "resources/faststats-onboarding.mixins.json",
        "1.16.2", "1.17", "1.19"
    ),
    SharedSource(
        "1.17", "java/dev/faststats/screen/minecraft/TextArea.java",
        "1.19"
    ),
    SharedSource(
        "1.19.3", "resources/faststats-onboarding.mixins.json",
        "1.19.4", "1.20", "1.20.2", "1.20.3"
    ),
    SharedSource(
        "1.19.4", "java/dev/faststats/screen/minecraft/mixin/TelemetryInfoScreenMixin.java",
        "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21.4", "1.21.9"
    ),
    SharedSource(
        "1.20", "java/dev/faststats/screen/minecraft/TextArea.java",
        "1.20.2", "1.20.3", "1.20.5"
    ),
    SharedSource(
        "1.20.5", "resources/faststats-onboarding.mixins.json",
        "1.21.4", "1.21.9"
    ),
    SharedSource(
        "1.21.4", "java/dev/faststats/screen/minecraft/ConsentScreen.java",
        "1.21.9"
    ),
)

sourceSets.main {
    java.srcDir(directory.resolve("src/main/java"))
    resources.srcDir(directory.resolve("src/main/resources"))

    sharedSources.filter { variant.name in it.users }.forEach { shared ->
        val root = shared.path.substringBefore('/')
        val source = objects.sourceDirectorySet("shared", "shared onboarding sources from ${shared.owner}")
        source.srcDir(rootProject.file("onboarding/versions/${shared.owner}/src/main/$root"))
        source.include(shared.path.substringAfter('/'))
        (if (root == "java") java else resources).source(source)
    }
}
