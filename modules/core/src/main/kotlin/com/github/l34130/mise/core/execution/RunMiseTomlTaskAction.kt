package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationProducer
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.psiLocation
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.readAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class RunMiseTomlTaskAction(
    private val miseTask: MiseTask,
) : AnAction(
        "Run Mise Task '${miseTask.name}'",
        "Execute the Mise Toml task",
        AllIcons.Actions.Execute,
    ) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        CoroutineScope(Dispatchers.Default).launch {
            val psiLocation = readAction { miseTask.psiLocation(project) } ?: return@launch

            var dataContext =
                SimpleDataContext.getSimpleContext(
                    Location.DATA_KEY,
                    psiLocation,
                    event.dataContext,
                )

            dataContext =
                SimpleDataContext.getSimpleContext(
                    MiseTask.DATA_KEY,
                    miseTask,
                    dataContext,
                )

            val context = ConfigurationContext.getFromContext(dataContext, event.place)

            val producer = MiseTomlTaskRunConfigurationProducer()
            val configuration: RunnerAndConfigurationSettings =
                readAction {
                    producer.findOrCreateConfigurationFromContext(context)?.configurationSettings
                        ?: event.project?.let { project ->
                            RunManager.getInstance(project).getConfigurationTemplate(producer.configurationFactory)
                        }
                } ?: return@launch

            val runManager = (context.runManager as? RunManagerEx) ?: return@launch
            runManager.setTemporaryConfiguration(configuration)
            ExecutionUtil.runConfiguration(configuration, Executor.EXECUTOR_EXTENSION_NAME.extensionList.first())
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
