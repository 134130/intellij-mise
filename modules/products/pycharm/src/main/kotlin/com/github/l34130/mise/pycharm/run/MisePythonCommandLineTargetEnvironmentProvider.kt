package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
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
        val projectState = project.service<MiseProjectSettings>().state
        if (!projectState.useMiseDirEnv) return

        val envs = MiseHelper.getMiseEnvVarsOrNotify(
            project = project,
            workDir = project.basePath,
            configEnvironment = projectState.miseConfigEnvironment,
        )

        for ((key, value) in runParams.envs + envs) {
            pythonExecution.addEnvironmentVariable(key, value)
        }
    }
}
