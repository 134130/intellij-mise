package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.icon.MiseIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import javax.swing.Icon

class MiseTomlTaskRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Mise Toml Task"

    override fun getConfigurationTypeDescription(): String = "Mise Toml Task run configuration"

    override fun getIcon(): Icon = MiseIcons.DEFAULT

    override fun getId(): String = javaClass.simpleName

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(MiseTomlTaskRunConfigurationFactory(this))

    companion object {
        fun getInstance(): MiseTomlTaskRunConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(MiseTomlTaskRunConfigurationType::class.java)
    }
}
