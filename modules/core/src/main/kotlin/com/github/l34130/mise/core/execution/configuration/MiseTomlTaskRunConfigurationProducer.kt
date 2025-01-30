package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.lang.psi.taskName
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable

class MiseTomlTaskRunConfigurationProducer : LazyRunConfigurationProducer<MiseTomlTaskRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        MiseTomlTaskRunConfigurationFactory(MiseTomlTaskRunConfigurationType.getInstance())

    override fun isConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
    ): Boolean = configuration.miseTaskName == findTaskName(context)

    override fun setupConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        if (context.psiLocation?.containingFile !is TomlFile) return false

        val taskName = findTaskName(context) ?: return false
        configuration.miseTaskName = taskName

        val macroManager = PathMacroManager.getInstance(context.project)

        val virtualFile = context.location?.virtualFile
        configuration.workingDirectory = macroManager.collapsePath(virtualFile?.parent?.path ?: context.project.basePath)

        configuration.name = "Run $taskName"

        return true
    }

    private fun findTaskName(context: ConfigurationContext): String? {
        val table = context.psiLocation?.parentOfType<TomlTable>() ?: return null
        return table.taskName
    }
}
