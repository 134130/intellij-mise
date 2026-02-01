import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Constraints
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformPluginsExtension
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory

plugins {
    id("java") // Java support
    id("org.jetbrains.intellij.platform")

    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIdeaExt) // IntelliJ Gradle IDEA Extension Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)
    implementation(libs.jackson.module.kotlin)



    intellijPlatform {
        create(
            type = IntelliJPlatformType.IntellijIdeaCommunity,
            version = providers.gradleProperty("platformVersion")
        ) {
            useInstaller = false
        }

        pluginComposedModule(implementation(project(":mise-products-clion")))
        pluginComposedModule(implementation(project(":mise-products-database")))
        pluginComposedModule(implementation(project(":mise-products-diagram")))
        pluginComposedModule(implementation(project(":mise-products-goland")))
        pluginComposedModule(implementation(project(":mise-products-gradle")))
        pluginComposedModule(implementation(project(":mise-products-idea")))
        pluginComposedModule(implementation(project(":mise-products-nodejs")))
        pluginComposedModule(implementation(project(":mise-products-nx")))
        pluginComposedModule(implementation(project(":mise-products-pycharm")))
        pluginComposedModule(implementation(project(":mise-products-rider")))
        pluginComposedModule(implementation(project(":mise-products-ruby")))
        pluginComposedModule(implementation(project(":mise-products-sh")))

        plugins(listOf())

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

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
            untilBuild = provider { null }
        }

        name = providers.gradleProperty("pluginName")
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels =
            providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html#intellijPlatform-buildSearchableOptions
    buildSearchableOptions = false
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
//        IntelliJPlatformType.CLion,
//        IntelliJPlatformType.GoLand,
//        IntelliJPlatformType.IntellijIdeaCommunity,
        IntelliJPlatformType.IntellijIdeaUltimate,
//        IntelliJPlatformType.WebStorm,
        IntelliJPlatformType.PyCharmCommunity,
//        IntelliJPlatformType.PyCharmProfessional,
//        IntelliJPlatformType.Rider,
    )

val IntelliJPlatformPluginsExtension.pluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}

fun IntelliJPlatformPluginsExtension.configureIdeaRunIdePlugins() {
    compatiblePlugin("org.toml.lang")
    compatiblePlugin("org.jetbrains.plugins.go")   // Go support (mise-goland.xml)
    compatiblePlugin("NodeJS")                      // NodeJS support (mise-nodejs.xml)
    compatiblePlugin("JavaScript")                  // JavaScript ecosystem
    compatiblePlugin("PythonCore")                  // Python support (mise-pycharm.xml)
    compatiblePlugin("org.jetbrains.plugins.ruby")  // Ruby support (mise-ruby.xml)
    bundledPlugin("com.intellij.database")       // Database support (mise-database.xml)
    compatiblePlugin("com.intellij.gradle")         // Gradle support (mise-gradle.xml)
    compatiblePlugin("com.jetbrains.sh")            // Shell script support (mise-sh.xml)
    // Causes constant errors in WSL projects.
    //compatiblePlugin("dev.nx.console")              // NX Console support (mise-nx.xml)
}

intellijPlatformTesting.runIde.register("runPyCharmCommunity") {
    type = IntelliJPlatformType.PyCharmCommunity
    version = "2025.1"
    useInstaller = false
    plugins {
        compatiblePlugin("org.toml.lang")
    }
}

intellijPlatformTesting.runIde.register("runIntellijIdeaUltimate") {
    type = IntelliJPlatformType.IntellijIdeaUltimate
    version = "2025.1"
    useInstaller = false

    plugins {
        configureIdeaRunIdePlugins()
    }
}

intellijPlatformTesting.runIde.register("runIntellijIdeaUltimate2025_3") {
    type = IntelliJPlatformType.IntellijIdeaUltimate
    version = "2025.3"
    useInstaller = false

    plugins {
        configureIdeaRunIdePlugins()
    }
}
