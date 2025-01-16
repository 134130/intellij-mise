import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.kotlin) // Kotlin support
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, properties("platformVersion"), false)

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("org.toml.lang")

        jetbrainsRuntime()

        testFramework(TestFrameworkType.Platform)
    }
}
