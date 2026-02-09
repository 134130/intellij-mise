package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.SystemProperties
import com.jetbrains.python.console.PyConsoleOptions
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider

@Suppress("UnstableApiUsage")
class MisePythonCommandLineTargetEnvironmentProvider : PythonCommandLineTargetEnvironmentProvider {
    override fun extendTargetEnvironment(
        project: Project,
        helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        runParams: PythonRunParams,
    ) {
        val envs =
            if (runParams is AbstractPythonRunConfiguration<*>) {
                MiseHelper.getMiseEnvVarsOrNotify(
                    configuration = runParams,
                    workingDirectory = runParams.workingDirectory,
                )
            } else {
                // Python console run params may return null workingDir; mirror PydevConsoleRunnerFactory fallback logic.
                val workingDirectory = resolveConsoleWorkingDirectory(project, runParams)
                MiseHelper.getMiseEnvVarsOrNotify(
                    project = project,
                    workingDirectory = workingDirectory,
                )
            }

        for ((key, value) in runParams.envs + envs) {
            pythonExecution.addEnvironmentVariable(key, value)
        }
    }

    private fun resolveConsoleWorkingDirectory(
        project: Project,
        runParams: PythonRunParams,
    ): String {
        // Console uses a target-env workingDir function, so this mirrors JetBrains' console resolution.
        val settingsWorkingDirectory =
            PyConsoleOptions.getInstance(project).pythonConsoleSettings.workingDirectory
                ?.takeIf { it.isNotBlank() }
        if (settingsWorkingDirectory != null) return settingsWorkingDirectory

        val moduleName = runParams.safeModuleName()?.takeIf { it.isNotBlank() }
        val module =
            moduleName?.let { ModuleManager.getInstance(project).findModuleByName(it) }
        val moduleRoot =
            module?.let { ModuleRootManager.getInstance(it).contentRoots.firstOrNull() }
        if (moduleRoot != null) return moduleRoot.path

        val projectRoot =
            ProjectRootManager.getInstance(project).contentRoots
                .firstOrNull { it.fileSystem is LocalFileSystem }
        if (projectRoot != null) return projectRoot.path

        return SystemProperties.getUserHome()
    }

    // Kotlin treats moduleName as non-null, but console params can return null in practice.
    private fun PythonRunParams.safeModuleName(): String? = runCatching { moduleName }.getOrNull()
}
