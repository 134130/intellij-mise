package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.execution.target.TargetEnvironment
import com.intellij.execution.target.TargetPlatform
import com.intellij.openapi.project.Project
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.extendEnvs
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider
import java.util.function.Function

@Suppress("UnstableApiUsage")
class MisePythonCommandLineTargetEnvironmentProvider : PythonCommandLineTargetEnvironmentProvider {
    override fun extendTargetEnvironment(
        project: Project,
        helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        runParams: PythonRunParams,
    ) {
        if (runParams is AbstractPythonRunConfiguration<*>) {
            val envs = MiseHelper.getMiseEnvVarsOrNotify(
                configuration = runParams,
                workingDirectory = runParams.workingDirectory,
            )

            for ((key, value) in runParams.envs + envs) {
                pythonExecution.addEnvironmentVariable(key, value)
            }
        } else {
            // For python script or python console which do not have RunConfiguration
            val envs = MiseHelper.getMiseEnvVarsOrNotify(
                project = project,
                workingDirectory = runParams.workingDirectory ?: project.guessMiseProjectPath()
            )

            val additionalEnvs = (runParams.envs + envs)
                .mapValues { (_, value) ->
                    Function<TargetEnvironment, String> { _ -> value }
                }

            pythonExecution.extendEnvs(additionalEnvs, TargetPlatform.CURRENT)
        }
    }
}
