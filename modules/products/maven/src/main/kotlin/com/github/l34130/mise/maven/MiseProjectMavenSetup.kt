package com.github.l34130.mise.maven

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
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
