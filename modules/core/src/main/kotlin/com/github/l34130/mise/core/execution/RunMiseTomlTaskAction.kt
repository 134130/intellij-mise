package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationProducer
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext

internal class RunMiseTomlTaskAction(
    private val miseTask: MiseTask,
) : AnAction(
        "Run Mise Task '${miseTask.name}'",
        "Execute the Mise Toml task",
        AllIcons.Actions.Execute,
    ) {
    override fun actionPerformed(event: AnActionEvent) {
        var dataContext =
            SimpleDataContext.getSimpleContext(
                Location.DATA_KEY,
                PsiLocation((miseTask as MiseTask.TomlTable).keySegment),
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
        val configuration = producer.findOrCreateConfigurationFromContext(context)?.configurationSettings ?: return

        val runManager = (context.runManager as? RunManagerEx) ?: return
        runManager.setTemporaryConfiguration(configuration)
        ExecutionUtil.runConfiguration(configuration, Executor.EXECUTOR_EXTENSION_NAME.extensionList.first())
    }
}
