import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.kotlin) // Kotlin support
}

dependencies {
    implementation(project(":mise-core"))

    // Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    intellijPlatform {
        create(IntelliJPlatformType.PyCharmCommunity, providers.gradleProperty("platformVersion"), false)

        bundledPlugins("PythonCore")

        instrumentationTools()
    }
}
