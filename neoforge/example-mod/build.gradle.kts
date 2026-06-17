plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

neoForge {
    version = "26.1.2.76"
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.13.2")
}

dependencies {
    implementation(project(":neoforge"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":neoforge").sourceSets["main"].output)
}
