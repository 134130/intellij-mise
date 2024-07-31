fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij")
    alias(libs.plugins.kotlin) // Kotlin support
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2024.1.4")
    type.set("PC")

    // Plugin Dependencies
    plugins.set(listOf("PythonCore:241.18034.55"))
}

dependencies {
    implementation(project(":mise-core"))
}