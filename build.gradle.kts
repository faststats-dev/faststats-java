import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("faststats.root-conventions")
    id("com.gradleup.shadow") version "9.6.1" apply false
    kotlin("jvm") version "2.4.20" apply false
}
