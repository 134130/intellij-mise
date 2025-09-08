package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.util.baseDirectory
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
        val task = context.dataContext.getData(MiseTask.DATA_KEY) ?: return false
        return configuration.miseTaskName == task.name && configuration.workingDirectory == PathUtil.getParentPath(task.source)
    }

    override fun setupConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val task = context.dataContext.getData(MiseTask.DATA_KEY) ?: return false
        configuration.miseTaskName = task.name
        configuration.workingDirectory = PathUtil.getParentPath(task.source)
        configuration.name = "Run ${task.name}"
        return true
    }
}
