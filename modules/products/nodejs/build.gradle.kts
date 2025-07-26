import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij.platform.module")
    alias(libs.plugins.kotlin) // Kotlin support
}

val IntelliJPlatformDependenciesExtension.pluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}

fun IntelliJPlatformDependenciesExtension.pluginsInLatestCompatibleVersion(vararg pluginIds: String) =
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

dependencies {
    implementation(project(":mise-core"))
    testImplementation(libs.junit)

    // Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    intellijPlatform {
        create(IntelliJPlatformType.WebStorm, properties("platformVersion"), false)

        bundledPlugins("JavaScript", "NodeJS")
        pluginsInLatestCompatibleVersion("deno")

        jetbrainsRuntime()
        testFramework(TestFrameworkType.Platform)
        testImplementation("org.opentest4j:opentest4j:1.3.0")
    }
}
