import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.10.2"
}

dependencyResolutionManagement {
    // Configure project's dependencies
    repositories {
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}

include(
    "modules/core",
    "modules/products/clion",
    "modules/products/database",
    "modules/products/diagram",
    "modules/products/goland",
    "modules/products/gradle",
    "modules/products/idea",
    "modules/products/nodejs",
    "modules/products/nx",
    "modules/products/pycharm",
    "modules/products/rider",
    "modules/products/ruby",
    "modules/products/sh",
)

rootProject.name = "mise"

rootProject.children.forEach {
    it.name = (it.name.replaceFirst("modules/", "mise/").replace("/", "-"))
}
