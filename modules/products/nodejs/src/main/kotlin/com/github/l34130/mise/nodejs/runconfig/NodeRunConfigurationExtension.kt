package com.github.l34130.mise.nodejs.runconfig

import com.github.l34130.mise.core.commands.MiseCmd
import com.github.l34130.mise.core.run.MiseRunConfigurationSettingsEditor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class NodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    companion object {
        private val LOG = Logger.getInstance(NodeRunConfigurationExtension::class.java)
    }

    override fun getEditorTitle(): String = MiseRunConfigurationSettingsEditor.EDITOR_TITLE

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> =
        MiseRunConfigurationSettingsEditor(configuration.project)

    override fun getSerializationId(): String = MiseRunConfigurationSettingsEditor.SERIALIZATION_ID

    override fun readExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.readExternal(runConfiguration, element)
    }

    override fun writeExternal(
        runConfiguration: AbstractNodeTargetRunProfile,
        element: Element,
    ) {
        MiseRunConfigurationSettingsEditor.writeExternal(runConfiguration, element)
    }

    override fun createLaunchSession(
        configuration: AbstractNodeTargetRunProfile,
        environment: ExecutionEnvironment,
    ): NodeRunConfigurationLaunchSession? {
        val miseState = MiseRunConfigurationSettingsEditor.getMiseRunConfigurationState(configuration)
        if (miseState?.useMiseDirEnv != true) {
            return null
        }

        return object : NodeRunConfigurationLaunchSession() {
            override fun addNodeOptionsTo(targetRun: NodeTargetRun) {
                val envs =
                    MiseCmd.loadEnv(
                        workDir = configuration.project.basePath,
                        miseProfile = miseState.miseProfile,
                        project = configuration.project,
                    )

                for ((key, value) in envs) {
                    targetRun.commandLineBuilder.addEnvironmentVariable(key, value)
                }
            }
        }
    }

    override fun isApplicableFor(configuration: AbstractNodeTargetRunProfile): Boolean = true
}
