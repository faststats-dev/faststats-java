extra.set("moduleName", "dev.faststats.onboarding")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    api(project(":config"))
    api(project(":core"))
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
}

subprojects {
    if (parent?.path != ":onboarding:versions") return@subprojects

    tasks.jar {
        manifest.attributes["MixinConfigs"] = "faststats-onboarding.mixins.json"
    }

    repositories { maven("https://maven.neoforged.net/releases") }

    dependencies {
        "compileOnly"("cpw.mods:modlauncher:10.0.9")
        "compileOnlyApi"(project(":onboarding"))
        "compileOnly"("net.fabricmc:fabric-loader:0.19.3")
        "compileOnly"("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
    }
}
