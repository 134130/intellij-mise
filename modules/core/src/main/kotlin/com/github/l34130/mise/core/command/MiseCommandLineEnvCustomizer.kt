package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.MiseEnvCustomizer
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CommandLineEnvCustomizer
import com.intellij.execution.wsl.WSLDistribution
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

        // Bail early if exePath or workingDirectory are null or blank. Without these we can't reliably
        // resolve project context (working directory) or safely inspect exePath (it may be unset).
        if (isMissingExePathOrWorkingDirectory(commandLine)) return

        // The IDE uses wsl.exe to explore the local wsl config. This is always run locally on Windows and is a pre-requisite for this
        // plugin's Project setup and WSL caching. It must not be customized because it can result in recursion and deadlock.
        if(isIdeWslExeCommand(commandLine)) {
            logger.trace(logMessage("Skipping environment customization of wsl commands.", commandLine))
            return
        }

        val project = MiseCommandLineHelper.resolveProjectFromCommandLine(commandLine)

        if (project == null ) {
            logger.trace(logMessage("Skipping environment customization, could not resolve project.", commandLine))
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

    /**
     * We can only safely customize when we have enough context to:
     * 1) resolve the owning project from the working directory, and
     * 2) avoid triggering exceptions from unset commandLine.exePath.
     */
    private fun isMissingExePathOrWorkingDirectory(commandLine: GeneralCommandLine) = MiseCommandLineHelper.safeGetExePath(commandLine).isNullOrBlank() || commandLine.workingDirectory == null

    /**
     * Skip IntelliJ Platform internal WSL infrastructure calls.
     *
     * The IDE invokes the canonical WSL binary returned by [WSLDistribution.findWslExe()] during WSL discovery/caching.
     * Customizing env for those commands is not needed and can cause re-entrancy/recursion into mise env resolution
     * while the platform is still initializing WSL state.
     */
    private fun isIdeWslExeCommand(commandLine: GeneralCommandLine): Boolean = Path(commandLine.exePath) == WSLDistribution.findWslExe()

    private fun logMessage(message: String, commandLine: GeneralCommandLine) = "$message (cmd=$commandLine, workingDirectory=${commandLine.workingDirectory})"
}
