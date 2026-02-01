import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.kotlin) // Kotlin support
}

dependencies {
    implementation(project(":mise-core"))
    testImplementation(libs.junit)

    // Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    intellijPlatform {
        create(
            IntelliJPlatformType.CLion,
            properties("platformVersion")
        ) { useInstaller = false }

        bundledPlugins("com.intellij.clion", "com.intellij.cidr.base", "com.intellij.nativeDebug")

        jetbrainsRuntime()

        testFramework(TestFrameworkType.Platform)
        testImplementation("org.opentest4j:opentest4j:1.3.0")
    }
}
