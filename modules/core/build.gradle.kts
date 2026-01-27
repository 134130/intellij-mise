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
        create(
            IntelliJPlatformType.IntellijIdeaCommunity,
            properties("platformVersion")
        ) { useInstaller = false }

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("org.toml.lang")
        bundledPlugin("Git4Idea")

        jetbrainsRuntime()

        testFramework(TestFrameworkType.Platform)
        testImplementation("org.opentest4j:opentest4j:1.3.0")
    }
}

tasks.test {
    if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        exclude("**/WslPathUtilsTest.class")
    }
}
