package com.github.l34130.mise.pycharm.run

import com.github.l34130.mise.core.MiseHelper
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.python.run.PythonRunConfiguration
import kotlinx.collections.immutable.toImmutableMap

class MisePythonExecutionListener : ExecutionListener {
    override fun processStarting(
        executorId: String,
        env: ExecutionEnvironment,
    ) {
        val pythonRunConfiguration = env.runProfile as? PythonRunConfiguration ?: return

        val envVars =
            MiseHelper.getMiseEnvVarsOrNotify(
                configuration = pythonRunConfiguration,
                workingDirectory = { pythonRunConfiguration.workingDirectory },
            )

        if (envVars.isEmpty()) {
            return super.processStarting(executorId, env)
        }

        executions[env.executionId] = pythonRunConfiguration.envs.toImmutableMap()
        pythonRunConfiguration.envs.putAll(envVars)

        super.processStarting(executorId, env)
    }

    override fun processStarted(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
    ) {
        restoreEnvVars(env)
        super.processStarted(executorId, env, handler)
    }

    override fun processNotStarted(
        executorId: String,
        env: ExecutionEnvironment,
    ) {
        restoreEnvVars(env)
        super.processNotStarted(executorId, env)
    }

    override fun processTerminating(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
    ) {
        restoreEnvVars(env)
        super.processTerminating(executorId, env, handler)
    }

    private fun restoreEnvVars(env: ExecutionEnvironment) {
        val originalEnvVars = executions[env.executionId] ?: return
        executions.remove(env.executionId)

        val pythonRunConfiguration = env.runProfile as? PythonRunConfiguration ?: return
        pythonRunConfiguration.envs = originalEnvVars.toMutableMap()
    }

    companion object {
        // Contains original environment variables
        private val executions: MutableMap<Long, Map<String, String>> = mutableMapOf()
    }
}
