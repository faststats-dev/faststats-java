extra.set("moduleName", "dev.faststats.onboarding")

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(17)
}

dependencies {
    api(project(":config"))
    api(project(":core"))
    compileOnlyApi(libs.jspecify)
}

val catalog = libs

subprojects {
    if (parent?.path != ":onboarding:versions") return@subprojects

    tasks.jar {
        manifest.attributes["MixinConfigs"] = "faststats-onboarding.mixins.json"
    }

    repositories { maven("https://maven.neoforged.net/releases") }

    dependencies {
        "compileOnly"(catalog.modlauncher)
        "compileOnlyApi"(project(":onboarding"))
        "compileOnly"(catalog.fabric.loader)
        "compileOnly"(catalog.sponge.mixin)
    }
}
