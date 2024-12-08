package com.github.l34130.mise.gradle.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class GradleEnvironmentProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean = task?.runProfile is ApplicationConfiguration

    override fun createExecutionEnvironment(
        project: Project,
        task: ExecuteRunConfigurationTask,
        executor: Executor,
    ): ExecutionEnvironment? {
        val environment =
            GradleExecutionEnvironmentProvider.EP_NAME.extensions
                .firstOrNull { provider ->
                    provider != this && provider.isApplicable(task)
                }?.createExecutionEnvironment(project, task, executor)

        if (environment?.runProfile !is GradleRunConfiguration) {
            return environment
        }
        val gradleRunConfiguration = environment.runProfile as GradleRunConfiguration

        val projectState = project.service<MiseSettings>().state
        val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(gradleRunConfiguration)

        val (workDir, profile) = when {
            projectState.useMiseDirEnv -> project.basePath to projectState.miseProfile
            runConfigState?.useMiseDirEnv == true -> {
                val sourceConfig = task.runProfile as ApplicationConfiguration
                sourceConfig.project.basePath to runConfigState.miseProfile
            }

            else -> return environment
        }

        val miseEnvVars = MiseCommandLineHelper.getEnvVars(workDir, profile)
            .fold(
                onSuccess = { envVars -> envVars },
                onFailure = {
                    if (it !is MiseCommandLineNotFoundException) {
                        MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                    }
                    emptyMap()
                },
            )

        gradleRunConfiguration.settings.env = miseEnvVars + gradleRunConfiguration.settings.env
        return environment
    }
}
