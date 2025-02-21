package com.github.l34130.mise.core.execution

import com.github.l34130.mise.core.execution.configuration.MiseTomlTaskRunConfigurationProducer
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
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
import com.intellij.openapi.vfs.findPsiFile

internal class RunMiseTomlTaskAction(
    private val miseTask: MiseTask,
) : AnAction(
        "Run Mise Task '${miseTask.name}'",
        "Execute the Mise Toml task",
        AllIcons.Actions.Execute,
    ) {
    override fun actionPerformed(event: AnActionEvent) {
        var dataContext =
            when (miseTask) {
                is MiseTask.ShellScript -> event.project?.let { project -> miseTask.file.findPsiFile(project) }?.let { PsiLocation(it) }
                is MiseTask.TomlTable -> PsiLocation(miseTask.keySegment)
                is MiseTask.Unknown -> null
            }?.let { SimpleDataContext.getSimpleContext(Location.DATA_KEY, it, event.dataContext) }

        dataContext =
            SimpleDataContext.getSimpleContext(
                MiseTask.DATA_KEY,
                miseTask,
                dataContext,
            )

        val context = ConfigurationContext.getFromContext(dataContext, event.place)

        val producer = MiseTomlTaskRunConfigurationProducer()
        val configuration: RunnerAndConfigurationSettings =
            producer.findOrCreateConfigurationFromContext(context)?.configurationSettings
                ?: RunManager.getInstance(context.project).getConfigurationTemplate(producer.configurationFactory)

        val runManager = (context.runManager as? RunManagerEx) ?: return
        runManager.setTemporaryConfiguration(configuration)
        ExecutionUtil.runConfiguration(configuration, Executor.EXECUTOR_EXTENSION_NAME.extensionList.first())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
