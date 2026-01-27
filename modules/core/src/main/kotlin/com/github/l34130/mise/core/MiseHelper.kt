package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.ConfigEnvironmentStrategy
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.project.Project

object MiseHelper {
    fun getMiseEnvVarsOrNotify(
        configuration: RunConfigurationBase<*>,
        workingDirectory: String?,
    ): Map<String, String> {
        val project = configuration.project
        val projectState = project.service<MiseProjectSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)

        // Check if disabled at run config level
        if (runConfigState?.useMiseDirEnv == false) return emptyMap()

        // Check master toggle AND run config setting
        if (!projectState.useMiseDirEnv || !projectState.useMiseInRunConfigurations) {
            return emptyMap()
        }

        val workDir = workingDirectory?.takeIf { it.isNotBlank() }
            ?: project.guessMiseProjectPath()

        val configEnvironment =
            if (runConfigState?.configEnvironmentStrategy == ConfigEnvironmentStrategy.OVERRIDE_PROJECT_SETTINGS) {
                runConfigState.miseConfigEnvironment
            } else {
                projectState.miseConfigEnvironment
            }

        return getMiseEnvVarsOrNotify(project, workDir, configEnvironment)
    }

    fun getMiseEnvVarsOrNotify(
        project: Project,
        workingDirectory: String,
        configEnvironment: String? = null,
    ): MutableMap<String, String> {
        val projectState = project.service<MiseProjectSettings>().state

        // Early return if disabled
        if (!projectState.useMiseDirEnv) {
            logger.debug { "Mise environment variables loading is disabled in project settings" }
            return mutableMapOf()
        }

        val resolvedConfigEnvironment = configEnvironment ?: projectState.miseConfigEnvironment

        // Cache handles fast-path + threading automatically
        val result = MiseCommandLineHelper.getEnvVars(project, workingDirectory, resolvedConfigEnvironment)
        return processEnvVarsResult(result, project)
    }

    /**
     * Process a Result<Map<String, String>> into a MutableMap with proper error handling.
     * Adds injection marker on success, shows notification on failure.
     */
    private fun processEnvVarsResult(
        result: Result<Map<String, String>>,
        project: Project
    ): MutableMap<String, String> {
        return result.fold(
            onSuccess = { envVars ->
                val mutableEnvVars = envVars.toMutableMap()
                MiseCommandLineHelper.environmentHasBeenCustomized(mutableEnvVars)
                mutableEnvVars
            },
            onFailure = { exception ->
                if (exception !is MiseCommandLineNotFoundException) {
                    MiseNotificationServiceUtils.notifyException(
                        "Failed to load environment variables",
                        exception,
                        project
                    )
                }
                mutableMapOf()
            },
        )
    }

    private val logger = Logger.getInstance(MiseHelper::class.java)
}
