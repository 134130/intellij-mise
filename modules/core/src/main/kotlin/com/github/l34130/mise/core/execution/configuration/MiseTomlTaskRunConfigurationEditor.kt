package com.github.l34130.mise.core.execution.configuration

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class MiseTomlTaskRunConfigurationEditor(
    project: Project,
) : SettingsEditor<MiseTomlTaskRunConfiguration>() {
    private val miseConfigEnvironmentTf = JBTextField()
    private val miseTaskNameTf = JBTextField()
    private val miseTaskArgsComponent = RawCommandLineEditor()
    private val workingDirectoryTf = TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    init {
        workingDirectoryTf.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.singleDir().withTitle("Working Directory").withDescription("Select the working directory"),
        )
    }

    override fun createEditor(): JComponent =
        panel {
            row("Mise config environment:") {
                cell(miseConfigEnvironmentTf).align(AlignX.FILL)
            }
            row("Mise task name:") {
                cell(miseTaskNameTf).align(AlignX.FILL)
            }
            row("Mise task arguments:") {
                cell(miseTaskArgsComponent).align(AlignX.FILL)
            }
            row("Working directory:") {
                cell(workingDirectoryTf).align(AlignX.FILL)
            }
            row(envVarsComponent.label) {
                cell(envVarsComponent.component).align(AlignX.FILL)
            }
        }.apply {
            border = JBUI.Borders.empty(6, 16, 16, 16)
        }

    override fun resetEditorFrom(configuration: MiseTomlTaskRunConfiguration) {
        miseConfigEnvironmentTf.text = configuration.miseConfigEnvironment
        miseTaskNameTf.text = configuration.miseTaskName
        miseTaskArgsComponent.text = configuration.taskParams
        workingDirectoryTf.text = configuration.workingDirectory
        envVarsComponent.envData = configuration.envVars
    }

    override fun applyEditorTo(configuration: MiseTomlTaskRunConfiguration) {
        configuration.miseConfigEnvironment = miseConfigEnvironmentTf.text
        configuration.miseTaskName = miseTaskNameTf.text
        configuration.workingDirectory = workingDirectoryTf.text
        configuration.envVars = envVarsComponent.envData
        configuration.taskParams = miseTaskArgsComponent.text
    }
}
