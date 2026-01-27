package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.waitForProjectCache
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Common interface for all mise environment customizers.
 * Provides shared logic for marker checking, settings validation, and environment customization.
 *
 * Implementations must:
 * 1. Provide a logger instance
 * 2. Implement shouldCustomizeForSettings() to check their specific setting flag
 * 3. Call customizeMiseEnvironment() with project, workDir, and environment map
 */
interface MiseEnvCustomizer {
    val logger: Logger

    /**
     * Determines if environment should be customized based on project settings.
     * Must check at minimum: settings.useMiseDirEnv && [specific setting flag]
     *
     * @param settings The project's mise settings
     * @return true if environment should be customized
     */
    fun shouldCustomizeForSettings(settings: MiseProjectSettings.MyState): Boolean

    /**
     * Performs the common customization logic: marker check → settings check → customize.
     *
     * This method encapsulates the shared behavior across all mise environment customizers:
     * - Checks if environment has already been customized (prevents double-injection)
     * - Validates settings using shouldCustomizeForSettings()
     * - Retrieves mise environment variables and adds them to the environment
     * - Handles errors with logging and cleanup
     *
     * @param project The project context
     * @param workDir Working directory for mise command execution
     * @param environment Environment map to customize (supports nullable keys/values for VCS API)
     * @return true if customization was attempted (even if it failed)
     */
    fun customizeMiseEnvironment(
        project: Project,
        workDir: String,
        environment: MutableMap<out String?, out String?>
    ): Boolean {
        val settings = project.service<MiseProjectSettings>().state
        if (!shouldCustomizeForSettings(settings)) return false
        if (!project.waitForProjectCache()) return false

        // 3. Customize with error handling
        try {
            val envVars = MiseHelper.getMiseEnvVarsOrNotify(project, workDir, settings.miseConfigEnvironment)
            @Suppress("UNCHECKED_CAST")
            (environment as MutableMap<String?, String?>).putAll(envVars)
            return true
        } catch (e: Exception) {
            logger.error("Failed to customize mise environment", e)
            MiseCommandLineHelper.environmentCustomizationFailed(environment)
            return false
        }
    }
}
