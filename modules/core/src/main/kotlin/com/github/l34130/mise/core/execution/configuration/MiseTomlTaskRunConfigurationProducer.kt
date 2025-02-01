package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.lang.psi.taskName
import com.github.l34130.mise.core.model.MiseTask
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTable

internal class MiseTomlTaskRunConfigurationProducer : LazyRunConfigurationProducer<MiseTomlTaskRunConfiguration>() {
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
        val tomlKeySegment = context.psiLocation as? TomlKeySegment ?: return false

        val task = MiseTask.TomlTable.resolveOrNull(tomlKeySegment) ?: return false
        configuration.miseTaskName = task.name

        val macroManager = PathMacroManager.getInstance(context.project)

        val virtualFile = context.location?.virtualFile
        configuration.workingDirectory = macroManager.collapsePath(virtualFile?.parent?.path ?: context.project.basePath)

        configuration.name = "Run ${task.name}"

        return true
    }

    private fun findTaskName(context: ConfigurationContext): String? {
        val table = context.psiLocation?.parentOfType<TomlTable>() ?: return null
        return table.taskName
    }
}
