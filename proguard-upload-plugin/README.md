# Proguard Mappings Upload Plugin

A Gradle plugin that uploads ProGuard/R8 obfuscation mapping files to the [Faststats](https://faststats.dev) sourcemaps
API for stacktrace deobfuscation.

## Installation

Add the FastStats releases repository to your project's settings:

```kotlin
pluginManagement {
    repositories {
        maven("https://repo.faststats.dev/releases")
        gradlePluginPortal()
    }
}
```

Or in Groovy (`settings.gradle`):

```groovy
pluginManagement {
    repositories {
        maven {
            url = uri('https://repo.faststats.dev/releases')
        }
        gradlePluginPortal()
    }
}
```

Add the plugin to your project's `build.gradle.kts`:

```kotlin
plugins {
    id("dev.faststats.proguard-mappings-upload") version "0.1.4"
}
```

Or in Groovy (`build.gradle`):

```groovy
plugins {
    id 'dev.faststats.proguard-mappings-upload' version '0.1.4'
}
```

## Configuration

### With a custom ProGuard task

```kotlin
mappingsUpload {
    authToken.set("your-auth-token")
    proguardTask.set(tasks.getByName("proguard"))
    mappingFiles.from(layout.buildDirectory.file("proguard/mapping.txt"))
}
```

Setting `proguardTask` ensures the upload task runs after ProGuard finishes. You still need to point `mappingFiles` to
the actual mapping file location (matching your `printmapping` config).

### Android Projects

```kotlin
mappingsUpload {
    authToken.set("your-auth-token")
}
```

The plugin automatically detects Android R8/ProGuard mapping file outputs when the Android Gradle Plugin is present. No
additional configuration is needed.

### All Options

```kotlin
mappingsUpload {
    // Required – API auth token. Falls back to FASTSTATS_AUTH_TOKEN env var.
    authToken.set("your-auth-token")

    // Optional – API endpoint (default: https://sourcemaps.faststats.dev/api/sourcemaps)
    endpoint.set("https://sourcemaps.faststats.dev/api/sourcemaps")

    // Optional – Build identifier (default: project.version)
    buildId.set("1.2.3")

    // Optional – Task that produces the mapping file (adds a dependsOn)
    proguardTask.set(tasks.getByName("proguard"))

    // Optional – Mapping files to upload
    mappingFiles.from(layout.buildDirectory.file("proguard/mapping.txt"))
}
```

## Usage

Run the upload task:

```bash
./gradlew uploadProguardMappings
```

Or chain it after your obfuscation task:

```bash
./gradlew proguard uploadProguardMappings
```

## CI Configuration

### GitHub Actions

```yaml
name: Build & Upload Mappings

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Build & Upload
        run: ./gradlew proguard uploadProguardMappings
        env:
          FASTSTATS_AUTH_TOKEN: ${{ secrets.FASTSTATS_AUTH_TOKEN }}
```

### GitLab CI

```yaml
build:
  stage: build
  script:
    - ./gradlew proguard uploadProguardMappings
  variables:
    FASTSTATS_AUTH_TOKEN: $FASTSTATS_AUTH_TOKEN
```

## How It Works

1. The plugin looks for mapping files added via `mappingFiles.from(...)`, or auto-detected Android build outputs.
2. If `proguardTask` is set, the upload task automatically depends on it.
3. Uses `project.version` as the `buildId` by default.
4. Each mapping file is split by class sections and uploaded in batches of up to 50MB, ensuring no class mapping is
   split across batches.

## Requirements

- Gradle 7.0+
- JDK 11+
