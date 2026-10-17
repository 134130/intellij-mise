package com.github.l34130.mise.maven

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.getWslDistribution
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.execution.ParametersListUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.project.MavenHomeType
import org.jetbrains.idea.maven.project.MavenInSpecificPath
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.StaticResolvedMavenHomeType
import org.jetbrains.idea.maven.utils.MavenUtil
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass

class MiseProjectMavenSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("maven")

    override suspend fun runSdkConfigurationCheck(
        project: Project,
        isUserInteraction: Boolean,
        forceRefresh: Boolean,
    ) {
        super.runSdkConfigurationCheck(project, isUserInteraction, forceRefresh)
        // Settings also apply to bundled Maven and wrappers, independently of the mise Maven version.
        withContext(Dispatchers.IO) {
            if (project.isDisposed) return@withContext
            val environment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
            MiseCommandLineHelper.getEnvVars(project, configEnvironment = environment)
                .mapCatching { env -> syncUserSettings(project, env["MAVEN_ARGS"]) }
                .onFailure {
                    if (it is CancellationException || it is ControlFlowException) throw it
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to sync Maven settings", it, project)
                    }
                }
        }
    }

    internal fun syncUserSettings(project: Project, mavenArgs: String?) {
        val settingsPath = mavenUserSettingsPath(mavenArgs) ?: return
        val localPath = project.getWslDistribution()?.let { distribution ->
            if (settingsPath.startsWith("/")) distribution.getWindowsPath(settingsPath) else settingsPath
        } ?: settingsPath
        val file = Path.of(project.guessMiseProjectPath()).resolve(localPath).normalize().toFile()
        require(file.isFile) { "Maven settings file does not exist: $file" }
        MavenProjectsManager.getInstance(project).generalSettings.setUserSettingsFile(file.path)
    }

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val mavenHomeType: MavenHomeType = mavenProjectsManager.generalSettings.mavenHomeType
        val resolvedInstallPath = resolveMavenHome(tool.resolvedInstallPath)

        val currentSdkVersion = when (mavenHomeType) {
            is MavenInSpecificPath -> {
                if (isSamePath(mavenHomeType.mavenHome, resolvedInstallPath)) {
                    return SdkStatus.UpToDate
                }
                MavenUtil.getMavenVersion(mavenHomeType) ?: mavenHomeType.title
            }

            // Bundled Maven
            is StaticResolvedMavenHomeType -> MavenUtil.getMavenVersion(mavenHomeType) ?: mavenHomeType.title

            // Maven Wrapper
            else -> mavenHomeType.title
        }

        return SdkStatus.NeedsUpdate(
            currentSdkVersion = currentSdkVersion,
            requestedInstallPath = resolvedInstallPath,
        )
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val mavenHome = resolveMavenHome(tool.resolvedInstallPath)
        val homeType = MavenInSpecificPath(mavenHome)
        mavenProjectsManager.generalSettings.mavenHomeType = homeType

        return ApplySdkResult(
            sdkName = "Maven",
            sdkVersion = MavenUtil.getMavenVersion(homeType) ?: tool.displayVersion,
            sdkPath = mavenHome,
        )
    }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null

    /**
     * Resolves the actual Maven home directory from a mise install path.
     *
     * Mise extracts the official Apache Maven tarball into a version directory,
     * which itself contains a single subdirectory named after the distribution:
     *
     * ```
     * ~/.local/share/mise/installs/maven/3.9.11/
     * └── apache-maven-3.9.11/   ← actual Maven home
     *     ├── bin/mvn
     *     ├── bin/m2.conf
     *     └── lib/maven-core-3.9.11.jar
     * ```
     *
     * IntelliJ's [MavenUtil.isValidMavenHome] expects the inner directory, not the
     * mise version root. If the given path is not a valid Maven home, this function
     * looks one level deeper for a subdirectory that is.
     */
    private fun resolveMavenHome(installPath: String): String {
        if (MavenUtil.isValidMavenHome(Path.of(installPath))) return installPath
        return File(installPath).listFiles()
            ?.firstOrNull { MavenUtil.isValidMavenHome(it.toPath()) }
            ?.absolutePath
            ?: installPath
    }

    private fun isSamePath(path1: String, path2: String): Boolean =
        FileUtil.filesEqual(File(path1), File(path2))
}

internal fun mavenUserSettingsPath(mavenArgs: String?): String? {
    val args = ParametersListUtil.parse(mavenArgs.orEmpty(), false, true, true)
    for ((index, arg) in args.withIndex()) {
        if (arg == "--") break
        val path = when {
            arg == "--settings" || arg == "-s" -> args.getOrNull(index + 1)
            arg.startsWith("--settings=") -> arg.substringAfter('=')
            arg.startsWith("-s=") -> arg.substringAfter('=')
            else -> continue
        }
        return path?.takeIf { it.isNotBlank() && !it.startsWith("-") }
    }
    return null
}
