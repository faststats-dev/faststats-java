repositories {
    maven("https://maven.neoforged.net/releases")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(project(":neoforge"))
    compileOnly("net.neoforged.fancymodloader:loader:10.0.36")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":neoforge").sourceSets["main"].output)
}
