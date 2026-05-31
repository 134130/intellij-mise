package com.github.l34130.mise.idea.run

import com.intellij.execution.ExecutionListener
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment

class MiseIdeaExecutionListener : ExecutionListener {
    override fun processNotStarted(
        executorId: String,
        env: ExecutionEnvironment,
    ) {
        restoreExternalSystemEnv(env)
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int,
    ) {
        restoreExternalSystemEnv(env)
    }

    private fun restoreExternalSystemEnv(env: ExecutionEnvironment) {
        val configuration = env.runProfile as? RunConfigurationBase<*> ?: return
        MiseIdeaRunConfigurationEnvironmentStore.restore(configuration)
    }
}
