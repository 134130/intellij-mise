package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.lang.MiseTomlFileType
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class MiseTomlTaskRunConfigurationEditor(
    private val project: Project,
) : SettingsEditor<MiseTomlTaskRunConfiguration>() {
    private val applicationState = application.service<MiseSettings>().state

    private val miseExecutablePathTf = TextFieldWithBrowseButton()
    private val miseConfigEnvironmentTf = JBTextField()
    private val miseTaskNameTf = JBTextField()
    private val workingDirectoryTf = TextFieldWithBrowseButton()
    private val envVarsComponent = EnvironmentVariablesComponent()

    init {
        miseExecutablePathTf.addBrowseFolderListener(
            "Mise Executable Path",
            "Select the Mise executable path",
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor(MiseTomlFileType),
        )
        workingDirectoryTf.addBrowseFolderListener(
            "Working Directory",
            "Select the working directory",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
    }

    override fun createEditor(): JComponent =
        panel {
            row("Mise executable:") {
                cell(miseExecutablePathTf).align(AlignX.FILL)
            }
            row("Mise config environment:") {
                cell(miseConfigEnvironmentTf).align(AlignX.FILL)
            }
            row("Mise task name:") {
                cell(miseTaskNameTf).align(AlignX.FILL)
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
        miseExecutablePathTf.text = configuration.miseExecutablePath
        miseConfigEnvironmentTf.text = configuration.miseConfigEnvironment
        miseTaskNameTf.text = configuration.miseTaskName
        workingDirectoryTf.text = configuration.workingDirectory ?: ""
        envVarsComponent.envData = configuration.envVars
    }

    override fun applyEditorTo(configuration: MiseTomlTaskRunConfiguration) {
        configuration.miseExecutablePath = miseExecutablePathTf.text
        configuration.miseConfigEnvironment = miseConfigEnvironmentTf.text
        configuration.miseTaskName = miseTaskNameTf.text
        configuration.workingDirectory = workingDirectoryTf.text
        configuration.envVars = envVarsComponent.envData
    }
}
