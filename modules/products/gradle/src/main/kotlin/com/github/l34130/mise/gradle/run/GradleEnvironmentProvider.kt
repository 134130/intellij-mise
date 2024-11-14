package com.github.l34130.mise.gradle.run

import com.github.l34130.mise.core.commands.MiseCmd
import com.github.l34130.mise.core.settings.MiseSettings
import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
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

        if (MiseSettings
                .getService(project)
                .state.useMiseDirEnv
                .not()
        ) {
            return environment
        }

        if (environment?.runProfile is GradleRunConfiguration) {
            val sourceConfig = task.runProfile as ApplicationConfiguration
            val gradleConfig = environment.runProfile as GradleRunConfiguration

            gradleConfig.settings.env = MiseCmd.loadEnv(
                workDir = sourceConfig.project.basePath,
                miseProfile = MiseSettings.getService(project).state.miseProfile,
                project = project,
            ) + sourceConfig.envs
        }

        return environment
    }
}
