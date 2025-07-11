import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
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
    alias(libs.plugins.detekt) // Gradle Detekt Plugin
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

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion"), false)

        pluginModule(implementation(project(":mise-products-clion")))
        pluginModule(implementation(project(":mise-products-diagram")))
        pluginModule(implementation(project(":mise-products-goland")))
        pluginModule(implementation(project(":mise-products-gradle")))
        pluginModule(implementation(project(":mise-products-idea")))
        pluginModule(implementation(project(":mise-products-nodejs")))
        pluginModule(implementation(project(":mise-products-pycharm")))
        pluginModule(implementation(project(":mise-products-rider")))
        pluginModule(implementation(project(":mise-products-ruby")))
        pluginModule(implementation(project(":mise-products-sh")))

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

val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif.json")
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        baseline = file("$rootDir/config/detekt/baseline.xml")
    }

    dependencies {
        detekt("io.gitlab.arturbosch.detekt:detekt-cli:${rootProject.libs.versions.detekt.get()}")
        detekt("io.gitlab.arturbosch.detekt:detekt-formatting:${rootProject.libs.versions.detekt.get()}")
        detekt(project(":mise-core"))
        detekt(project(":mise-products-clion"))
        detekt(project(":mise-products-diagram"))
        detekt(project(":mise-products-goland"))
        detekt(project(":mise-products-gradle"))
        detekt(project(":mise-products-idea"))
        detekt(project(":mise-products-nodejs"))
        detekt(project(":mise-products-pycharm"))
        detekt(project(":mise-products-rider"))
        detekt(project(":mise-products-ruby"))
        detekt(project(":mise-products-sh"))
    }

    tasks.withType<Detekt>().configureEach {
        enabled = false
        jvmTarget = "21"
        reports {
            xml.required = true
            html.required = true
            sarif.required = true
            md.required = true
        }
        basePath = rootDir.absolutePath
    }
    detektReportMergeSarif {
        input.from(tasks.withType<Detekt>().map { it.reports.sarif.outputLocation })
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = "21"
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
//        IntelliJPlatformType.PyCharmCommunity,
//        IntelliJPlatformType.PyCharmProfessional,
//        IntelliJPlatformType.Rider,
    )

val IntelliJPlatformPluginsExtension.pluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}

fun IntelliJPlatformPluginsExtension.pluginsInLatestCompatibleVersion(vararg pluginIds: String) =
    plugins(
        provider {
            pluginIds.map { pluginId ->
                val platformType = intellijPlatform.productInfo.productCode
                val platformVersion = intellijPlatform.productInfo.buildNumber

                val plugin =
                    pluginRepository.pluginManager
                        .searchCompatibleUpdates(
                            build = "$platformType-$platformVersion",
                            xmlIds = listOf(pluginId),
                        ).firstOrNull()
                        ?: throw GradleException(
                            "No plugin update with id='$pluginId' compatible with '$platformType-$platformVersion' found in JetBrains Marketplace",
                        )

                "${plugin.pluginXmlId}:${plugin.version}"
            }
        },
    )

runIdePlatformTypes.forEach { platformType ->
    intellijPlatformTesting.runIde.register("run${platformType.name}") {
        type = platformType
        version = "2025.1"

        plugins {
//            plugin("pluginId", "1.0.0")
            pluginsInLatestCompatibleVersion("org.toml.lang")
            disablePlugin("bundledPluginId")
        }
    }
}
