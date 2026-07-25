plugins {
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT"
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

dependencies {
    implementation(project(":fabric:versions:26.1-26.3"))
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc.fabric-api:fabric-api:0.155.3+26.3")
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":fabric").sourceSets["main"].output)
    from(project(":fabric:versions:26.1-26.3").sourceSets["main"].output)
}
