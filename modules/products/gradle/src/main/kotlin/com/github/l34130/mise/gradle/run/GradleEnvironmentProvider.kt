package com.github.l34130.mise.gradle.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleEnvironmentProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean = task?.runProfile is GradleRunConfiguration

    override fun createExecutionEnvironment(
        project: Project,
        task: ExecuteRunConfigurationTask,
        executor: Executor,
    ): ExecutionEnvironment? {
        val runProfile = task.runProfile as GradleRunConfiguration
        var environment: ExecutionEnvironment? = delegateProvider(task)?.createExecutionEnvironment(project, task, executor)

        // No registered environment provider. Gradle Task is in this case.
        if (environment == null) {
            environment =
                ExternalSystemUtil.createExecutionEnvironment(
                    runProfile.project,
                    GradleConstants.SYSTEM_ID,
                    runProfile.settings.clone(),
                    executor.id,
                )
        }

        // Code Coverage runs maybe in this case.
        if (environment == null) {
            val runner = ProgramRunner.getRunner(executor.id, task.runProfile)
            val taskSettings = task.settings
            if (runner != null && taskSettings != null) {
                environment =
                    ExecutionEnvironment(executor, runner, taskSettings, project)
            }
        }

        if (environment?.runProfile is GradleRunConfiguration) {
            val runProfile = task.runProfile as GradleRunConfiguration
            val projectState = project.service<MiseSettings>().state
            val runConfigState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(runProfile)

            val (workDir, configEnvironment) =
                when {
                    projectState.useMiseDirEnv -> project.basePath to projectState.miseConfigEnvironment
                    runConfigState?.useMiseDirEnv == true -> {
                        (runProfile.projectPathOnTarget ?: runProfile.project.basePath) to
                            runConfigState.miseConfigEnvironment
                    }

                    else -> return environment
                }

            val miseEnvVars =
                MiseCommandLineHelper
                    .getEnvVars(workDir, configEnvironment)
                    .fold(
                        onSuccess = { envVars -> envVars },
                        onFailure = {
                            if (it !is MiseCommandLineNotFoundException) {
                                MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                            }
                            emptyMap()
                        },
                    )

            val settings = (environment.runProfile as GradleRunConfiguration).settings
            settings.env = settings.env + miseEnvVars
        }

        return environment
    }

    private fun delegateProvider(task: ExecuteRunConfigurationTask): GradleExecutionEnvironmentProvider? {
        val extensions = GradleExecutionEnvironmentProvider.EP_NAME.extensions
        return extensions.firstOrNull { it !== this && it.isApplicable(task) }
    }
}
