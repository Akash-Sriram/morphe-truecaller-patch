rootProject.name = "morphe-truecaller-patch"

pluginManagement {
    repositories {
        maven { url = uri("${rootDir}/local-repo") }
        mavenLocal()
        gradlePluginPortal()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "1.2.0"
}

include(":patches:stub")
