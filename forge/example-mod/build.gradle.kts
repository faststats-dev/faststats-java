repositories {
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(project(":forge"))
    compileOnly("net.minecraftforge:fmlloader:1.21-51.0.21")
    compileOnly("net.minecraftforge:forgespi:7.1.4")
    compileOnly("net.minecraftforge:javafmllanguage:1.21-51.0.21")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":forge").sourceSets["main"].output)
}
