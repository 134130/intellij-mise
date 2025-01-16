package com.github.l34130.mise.core.execution.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.toml.lang.psi.TomlKeySegment

class MiseTomlTaskRunConfigurationFactory(
    private val runConfigurationType: MiseTomlTaskRunConfigurationType,
) : ConfigurationFactory(runConfigurationType) {
    override fun getId(): String = "MiseTomlTask"

    override fun getName(): String = "Mise Toml Task"

    override fun createTemplateConfiguration(project: Project): RunConfiguration = MiseTomlTaskRunConfiguration(project, this, "name")

    fun createConfigurationFromMiseTomlTask(segment: TomlKeySegment): MiseTomlTaskRunConfiguration? {
        TODO()
    }
}
