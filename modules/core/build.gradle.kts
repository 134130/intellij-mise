fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij")
    alias(libs.plugins.kotlin) // Kotlin support
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set(properties("platformVersion"))
}
