repositories {
    maven("https://maven.quiltmc.org/repository/release/")
}

dependencies {
    implementation(project(":quilt"))
    compileOnly("org.quiltmc:quilt-loader:0.29.1")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":config").sourceSets["main"].output)
    from(project(":core").sourceSets["main"].output)
    from(project(":quilt").sourceSets["main"].output)
}
