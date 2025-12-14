package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.intellij.openapi.project.Project
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
                // For python script or python console which do not have RunConfiguration
                MiseHelper.getMiseEnvVarsOrNotify(
                    project = project,
                    workingDirectory = runParams.workingDirectory,
                )
            }

        for ((key, value) in runParams.envs + envs) {
            pythonExecution.addEnvironmentVariable(key, value)
        }
    }
}
