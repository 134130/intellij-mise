package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

internal class MiseTomlTaskRunConfigurationProducer : LazyRunConfigurationProducer<MiseTomlTaskRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        MiseTomlTaskRunConfigurationFactory(MiseTomlTaskRunConfigurationType.getInstance())

    override fun isConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val task = context.dataContext.getData(MiseTask.DATA_KEY) ?: return false
        return configuration.miseTaskName == task.name
    }

    override fun setupConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val task = context.dataContext.getData(MiseTask.DATA_KEY) ?: return false
        configuration.miseTaskName = task.name

        val macroManager = PathMacroManager.getInstance(context.project)

        val virtualFile = context.location?.virtualFile
        configuration.workingDirectory = macroManager.collapsePath(virtualFile?.parent?.path ?: context.project.basePath) // FIXME: path is not resolved correctly

        configuration.name = "Run ${task.name}"

        return true
    }
}
