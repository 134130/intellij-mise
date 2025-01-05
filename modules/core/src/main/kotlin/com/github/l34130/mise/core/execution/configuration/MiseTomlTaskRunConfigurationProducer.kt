package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.lang.psi.MiseTomlFile
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class MiseTomlTaskRunConfigurationProducer : LazyRunConfigurationProducer<MiseTomlTaskRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        MiseTomlTaskRunConfigurationFactory(MiseTomlTaskRunConfigurationType.getInstance())

    override fun isConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val macroManager = PathMacroManager.getInstance(context.project)
//        return macroManager.expandPath(configuration.filename) == context.location?.virtualFile?.path &&
//                configuration.target == findTarget(context)?.name

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: MiseTomlTaskRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        if (context.psiLocation?.containingFile !is MiseTomlFile) return false

        val macroManager = PathMacroManager.getInstance(context.project)
        val path = context.location?.virtualFile?.path

        // edit configuration

        return true
    }
}
