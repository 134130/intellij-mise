package com.github.l34130.mise.gradle.run

import com.github.l34130.mise.core.MiseHelper
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

class MiseGradleEnvironmentProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean = task?.runProfile is GradleRunConfiguration

    override fun createExecutionEnvironment(
        project: Project,
        task: ExecuteRunConfigurationTask,
        executor: Executor,
    ): ExecutionEnvironment? {
        val runProfile = task.runProfile as? GradleRunConfiguration ?: return null
        var environment = delegateProvider(task)?.createExecutionEnvironment(project, task, executor)

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
            val miseEnvVars =
                MiseHelper.getMiseEnvVarsOrNotify(
                    configuration = runProfile,
                    workingDirectory = resolveWorkingDirectory(project, runProfile),
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

    private fun resolveWorkingDirectory(
        project: Project,
        runProfile: GradleRunConfiguration,
    ): String? {
        val targetProjectPath = runProfile.projectPathOnTarget
        val hostProjectPath = runProfile.settings.externalProjectPath ?: return targetProjectPath

        val gradleProjectPath = runProfile.settings.taskNames
            .firstOrNull()
            ?.let { gradleProjectPathFromTask(it) }
            ?: return targetProjectPath

        if (gradleProjectPath == ":") return targetProjectPath

        val hostSubProjectDir = findModuleProjectDir(project, hostProjectPath, gradleProjectPath)
            ?: return targetProjectPath

        return mapHostPathToTarget(hostSubProjectDir, hostProjectPath, targetProjectPath)
    }

    private fun gradleProjectPathFromTask(taskName: String): String {
        // ":foo:bar" -> ":foo"; "bar" -> ":"; ":bar" -> ":"
        val withoutTask = taskName.substringBeforeLast(':', missingDelimiterValue = "")
        return if (withoutTask.isEmpty()) ":" else withoutTask
    }

    private fun findModuleProjectDir(
        project: Project,
        externalProjectPath: String,
        gradleProjectPath: String,
    ): String? {
        val projectInfo = ProjectDataManager.getInstance()
            .getExternalProjectData(project, GradleConstants.SYSTEM_ID, externalProjectPath)
            ?: return null
        val structure = projectInfo.externalProjectStructure ?: return null

        val moduleNodes = ExternalSystemApiUtil.findAllRecursively(structure, ProjectKeys.MODULE)
        val moduleNode = moduleNodes.firstOrNull { node ->
            // Gradle's ModuleData.id is either ":foo:bar" or "<rootName>:foo:bar".
            val id = node.data.id
            id == gradleProjectPath || id.endsWith(gradleProjectPath)
        }
        return moduleNode?.data?.linkedExternalProjectPath
    }

    private fun mapHostPathToTarget(
        hostPath: String,
        hostRoot: String,
        targetRoot: String,
    ): String {
        if (hostPath == hostRoot) return targetRoot
        val normalizedHostRoot = hostRoot.trimEnd('/', '\\')
        if (!hostPath.startsWith(normalizedHostRoot)) return targetRoot
        val relative = hostPath
            .removePrefix(normalizedHostRoot)
            .trimStart('/', '\\')
            .replace('\\', '/')
        if (relative.isEmpty()) return targetRoot
        return targetRoot.trimEnd('/', '\\') + "/" + relative
    }
}
