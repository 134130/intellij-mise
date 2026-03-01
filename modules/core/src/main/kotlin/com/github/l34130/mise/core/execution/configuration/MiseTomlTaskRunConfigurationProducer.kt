package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.model.MiseTomlTableTask
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.util.PathUtil

internal class MiseTomlTaskRunConfigurationProducer : LazyRunConfigurationProducer<MiseTomlTaskRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        MiseTomlTaskRunConfigurationFactory(MiseTomlTaskRunConfigurationType.getInstance())

    override fun isConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val task = resolveTaskFromContext(context) ?: return false
        return configuration.miseTaskName == task.name && configuration.workingDirectory == PathUtil.getParentPath(task.source)
    }

    override fun setupConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val task = resolveTaskFromContext(context) ?: return false
        configuration.miseTaskName = task.name
        configuration.workingDirectory = PathUtil.getParentPath(task.source)
        configuration.name = "Run ${task.name}"
        return true
    }

    private fun resolveTaskFromContext(context: ConfigurationContext): MiseTask? {
        // First, try to get the task from the data context (used by tool window actions)
        context.dataContext.getData(MiseTask.DATA_KEY)?.let { return it }

        // If not available, try to resolve from the PSI element at the caret position
        val psiElement = context.psiLocation ?: return null

        // Try current element and related elements to find a task
        // This handles cases where the caret is on different parts of the task definition
        val elementsToCheck = mutableListOf(psiElement)
        psiElement.parent?.let { elementsToCheck.add(it) }
        psiElement.firstChild?.let { elementsToCheck.add(it) }

        for (element in elementsToCheck) {
            // Try to resolve from chained table format: [tasks.foo]
            MiseTomlTableTask.resolveFromTaskChainedTable(element)?.let { return it }

            // Try to resolve from inline table format: [tasks] foo = { ... }
            MiseTomlTableTask.resolveFromInlineTableInTaskTable(element)?.let { return it }
        }

        return null
    }
}
