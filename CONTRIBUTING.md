# Contributing

## Building

Run Gradle from the repository root:

```sh
./gradlew build
```

This compiles, tests, and packages every module, including the Fabric, Forge and NeoForge artifacts for every supported
Minecraft version. To work on a single version, build only its project:

```sh
./gradlew :fabric:versions:1.21.10:build
./gradlew :forge:versions:1.21.10:build
./gradlew :neoforge:versions:1.21.10:build
```

## Minecraft versions

Fabric, Forge and NeoForge are built and published once per Minecraft version, as
`dev.faststats.metrics:<fabric|forge|neoforge>:<sdk-version>+mc<minecraft-version>`. Each artifact only declares the
Minecraft version it was built for.

### Where the code lives

| Directory                        | Contents                                                     |
|----------------------------------|--------------------------------------------------------------|
| `fabric/`, `forge/`, `neoforge/` | Platform code that is the same for every Minecraft version   |
| `onboarding/`                    | Onboarding code without any Minecraft dependency             |
| `fabric/versions/<version>`      | Fabric code that depends on the Minecraft version            |
| `forge/versions/<version>`       | Forge code that depends on the Minecraft version             |
| `neoforge/versions/<version>`    | NeoForge code that depends on the Minecraft version          |
| `onboarding/versions/<version>`  | The consent screen and its mixins, compiled into all of them |

The directories in `<platform>/versions` are called variants. A variant is named after the first Minecraft version
that needs its code, and every later version is built from it until the next variant. For example, Fabric 1.21.10 is
built from `fabric/versions/1.21.9` and `onboarding/versions/1.21.9`.

Each Minecraft version still gets its own Gradle project, `:<platform>:versions:<version>`, which compiles the variants
against exactly that version. Their directories in `<platform>/versions` are created by Gradle and only contain build
output.

The onboarding sources are compiled into the Fabric, Forge and NeoForge projects rather than built on their own. Files that
are identical between onboarding variants only exist in the oldest variant using them. The other variants reference
them through the `sharedSources` table in `build-logic/src/main/kotlin/faststats.onboarding-sources.gradle.kts`.

### Naming

- **Variant directories:** the Minecraft version, e.g. `1.21.9` or `26.1`.
- **Fabric, Forge and NeoForge packages:** the variant with underscores, e.g. `dev.faststats.fabric.compat.v1_21_9`. Each
  variant registers its `CompatibilityLayerImpl` in
  `src/main/resources/META-INF/services/dev.faststats.<platform>.compat.CompatibilityLayer`.
- **Onboarding packages:** the same in every variant, `dev.faststats.screen.minecraft` and
  `dev.faststats.screen.minecraft.mixin`, so files can be shared between variants.
- **Version catalog entries:** the Minecraft version with `x` instead of dots, e.g. `minecraft-v1x21x10`,
  `fabric-api-v1x21x10`, `forge-v1x21x10` and `neoforge-v1x21x10`.

## Adding a Minecraft version

1. Add the version to `gradle/libs.versions.toml` (the release numbers below are placeholders):

   ```toml
   [versions]
   forge-v26x4 = "26.4-67.0.0"
   neoforge-v26x4 = "26.4.0.1"

   [libraries]
   minecraft-v26x4 = "com.mojang:minecraft:26.4"
   fabric-api-v26x4 = "net.fabricmc.fabric-api:fabric-api:0.160.0+26.4"
   ```

2. Add it to `minecraftVersions` in `build-logic/src/main/kotlin/MinecraftVersions.kt`, after the newest version:

   ```kotlin
   MinecraftVersion("26.4", java = 25, obfuscated = false),
   ```

   - `java`: the Java version the game requires. The code is compiled for it.
   - `obfuscated`: whether the game is obfuscated and has to be remapped. Versions before 26.1 are.
   - `fabricApi`: only needed when there is no Fabric API release for exactly this version, e.g.
     `fabricApi = "26.1.2"`.

3. Build the new version:

   ```sh
   ./gradlew :fabric:versions:26.4:build :forge:versions:26.4:build :neoforge:versions:26.4:build
   ```

If it builds, the version is done. It is published with the next release.

### When the code needs changes

If a version doesn't compile, or needs different behavior, give it its own variant on the platform that changed:

1. Copy the closest older variant, e.g. `fabric/versions/26.1` to `fabric/versions/26.4`.
2. For Fabric, Forge and NeoForge, rename the package to the new variant, e.g. `v26_1` to `v26_4`, and update the class name
   in its `META-INF/services` file.
3. For onboarding, the new variant only needs the files that change. For every file it takes unchanged from an older
   variant, add it to that file's `sharedSources` entry, or add an entry if the file doesn't have one yet.
4. Fix the code and build every version from the new one onwards, as they now use the new variant too.

A variant only replaces the code of its own platform. A new Fabric variant doesn't affect Forge, NeoForge or onboarding, and
the other way around.
