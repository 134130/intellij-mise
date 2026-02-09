package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.MiseEnvCustomizer
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.getWslDistribution
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CommandLineEnvCustomizer
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlin.io.path.Path

/**
 * Base command line customizer that customizes mise environment variables.
 *
 * This class can be extended by specific customizers (e.g., NX, Gradle) to filter
 * by executable name and customize settings checks.
 *
 * When used directly, it acts as a catch-all for terminal commands, external tools,
 * build tools, etc. Controlled by the "Use in all other command line execution" setting.
 *
 * Double-customization is prevented by checking for the marker added by
 * MiseHelper.getMiseEnvVarsOrNotify().
 */
@Suppress("UnstableApiUsage")
open class MiseCommandLineEnvCustomizer : CommandLineEnvCustomizer, MiseEnvCustomizer {
    override val logger = Logger.getInstance(MiseCommandLineEnvCustomizer::class.java)

    override fun customizeEnv(
        commandLine: GeneralCommandLine,
        environment: MutableMap<String, String>,
    ) {
        // Immediately exit if this isn't required. This must always be the first check to avoid recursion
        if (!MiseCommandLineHelper.environmentNeedsCustomization(commandLine.environment)) return

        // Safe exePath check, this prevents an exception being thrown by a null exePath or workingDirectory.
        // If we don't have both, we can't customize anyway.
        if (hasNullExePathOrWorkingDirectory(commandLine)) return

        // The IDE uses wsl.exe to explore the local wsl config. This is always run locally on Windows and is a pre-requisit for this
        // plugin's Project setup and WSL caching. It must not be customized.
        if(isInternalWslExeCommand(commandLine)) {
            logger.trace(logMessage("Skipping environment customization of wsl commands.", commandLine))
            return
        }

        val project = MiseCommandLineHelper.resolveProjectFromCommandLine(commandLine)

        if (project == null ) {
            logger.trace(logMessage("Skipping environment customization, could not resolve project.", commandLine))
            return
        }

        if (isNotRunningOnProjectSystem(commandLine, project)) {
            logger.trace(logMessage("Skipping environment customization of command on different system from project mise executable.", commandLine))
            return
        }

        // Perform checks that need the project (can be overridden by subclasses)
        if (!shouldCustomizeForProject(project, commandLine)) return

        val workDir = MiseCommandLineHelper.resolveWorkingDirectory(commandLine, project)

        logger.trace(logMessage("Customizing.", commandLine))
        // Perform checks against settings (override shouldCustomizeForSettings in subclasses if needed) and if good do the actual customization
        customizeMiseEnvironment(project, workDir, environment)
    }

    /**
     * Hook for subclasses to check if mise commands should be skipped.
     * Default: skip mise commands to prevent infinite recursion.
     * Subclasses can override to skip this check if already filtered by executable name.
     */
    protected open fun shouldCustomizeForProject(project: Project, commandLine: GeneralCommandLine): Boolean {
        return !(project.waitForProjectCache() && project.service<MiseExecutableManager>().matchesMiseExecutablePath(commandLine))
    }

    /**
     * Hook for subclasses to customize settings validation.
     * Default: check master toggle and "use in all command lines" setting.
     * Subclasses override this to check their specific setting flag.
     */
    override fun shouldCustomizeForSettings(settings: MiseProjectSettings.MyState): Boolean {
        return settings.useMiseDirEnv && settings.useMiseInAllCommandLines
    }

    private fun hasNullExePathOrWorkingDirectory(commandLine: GeneralCommandLine) = MiseCommandLineHelper.safeGetExePath(commandLine).isNullOrBlank() || commandLine.workingDirectory == null

    private fun isInternalWslExeCommand(commandLine: GeneralCommandLine): Boolean = Path(commandLine.exePath) == WSLDistribution.findWslExe()

    private fun isNotRunningOnProjectSystem(commandLine: GeneralCommandLine, project: Project): Boolean {
        return commandLine.workingDirectory != null && project.getWslDistribution()?.msId != WslPath.getDistributionByWindowsUncPath(commandLine.exePath)?.msId
    }

    private fun logMessage(message: String, commandLine: GeneralCommandLine) = "$message (cmd=$commandLine, workingDirectory=${commandLine.workingDirectory})"
}
