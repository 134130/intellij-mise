fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij")
    alias(libs.plugins.kotlin) // Kotlin support
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set(properties("platformVersion"))

    // Plugin Dependencies
    plugins.set(listOf("com.intellij.java", "com.intellij.gradle"))
}

dependencies {
    implementation(project(":mise-core"))
}
