import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.kotlin) // Kotlin support
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, properties("platformVersion"), false)

        bundledPlugin("com.intellij.java")

        instrumentationTools()
    }
}
