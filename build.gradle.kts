import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Constraints
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java") // Java support
    id("org.jetbrains.intellij.platform") // IntelliJ Platform Gradle Plugin

    alias(libs.plugins.gradleIdeaExt) // IntelliJ Gradle IDEA Extension Plugin
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
//    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2022.3.3", false)

        pluginModule(implementation(project(":mise-products-goland")))
        pluginModule(implementation(project(":mise-products-gradle")))
        pluginModule(implementation(project(":mise-products-idea")))
        pluginModule(implementation(project(":mise-products-nodejs")))
        pluginModule(implementation(project(":mise-products-pythoncore")))
        pluginModule(implementation(project(":mise-products-pythonid")))

        plugins(listOf())

        instrumentationTools()
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("platformVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes =
            providers.gradleProperty("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }

        name = providers.gradleProperty("pluginName")
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

val runIdeForUnitTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders +=
            CommandLineArgumentProvider {
                listOf(
                    "-Drobot-server.port=8082",
                    "-Dide.mac.message.dialogs.as.sheets=false",
                    "-Djb.privacy.policy.text=<!--999.999-->",
                    "-Djb.consents.confirmation.enabled=false",
                )
            }
    }

    plugins {
        robotServerPlugin(Constraints.LATEST_VERSION)
    }
}

val runIdePlatformTypes =
    listOf(
        IntelliJPlatformType.GoLand,
        IntelliJPlatformType.IntellijIdeaCommunity,
        IntelliJPlatformType.IntellijIdeaUltimate,
        IntelliJPlatformType.WebStorm,
        IntelliJPlatformType.PyCharmCommunity,
        IntelliJPlatformType.PyCharmProfessional,
    )

runIdePlatformTypes.forEach { platformType ->
    intellijPlatformTesting.runIde.register("run${platformType.name}") {
        type = platformType
        version = "2024.1"

        plugins {
//            plugin("pluginId", "1.0.0")
            disablePlugin("bundledPluginId")
        }
    }
}
