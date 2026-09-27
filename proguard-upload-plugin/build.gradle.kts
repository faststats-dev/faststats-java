plugins {
    kotlin("jvm") version "2.4.0"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "dev.faststats"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    compileOnly("com.android.tools.build:gradle:9.2.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    plugins {
        create("proguardMappingsUpload") {
            id = "dev.faststats.proguard-mappings-upload"
            implementationClass = "dev.faststats.proguard.FastStatsProguardPlugin"
            displayName = "Faststats ProGuard Mapping Upload Plugin"
            description = "Uploads ProGuard/R8 obfuscation mapping files to the FastStats sourcemaps API"
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom.scm {
            val repository = "FastStats-dev/faststats-java"
            url.set("https://github.com/$repository/tree/main/packages/proguard-upload-plugin")
            connection.set("scm:git:git://github.com/$repository.git")
            developerConnection.set("scm:git:ssh://github.com/$repository.git")
        }
    }
    repositories.maven {
        val branch = if (version.toString().contains("-pre")) "snapshots" else "releases"
        url = uri("https://repo.faststats.dev/$branch")
        credentials {
            username = System.getenv("REPOSITORY_USER")
            password = System.getenv("REPOSITORY_TOKEN")
        }
    }
}