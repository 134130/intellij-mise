package com.github.l34130.mise.core.setting

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithHistoryWithBrowseButton
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MiseConfigurable(
    private val project: Project,
) : SearchableConfigurable {
    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise")
    private val myMiseConfigEnvironmentTf = JBTextField()

    override fun getDisplayName(): String = "Mise Settings"

    override fun createComponent(): JComponent {
        val service = MiseSettings.getService(project)

        myMiseDirEnvCb.isSelected = service.state.useMiseDirEnv
        myMiseConfigEnvironmentTf.text = service.state.miseConfigEnvironment

        return JPanel(BorderLayout()).apply {
            add(
                panel {
                    row("Mise Executable:") {
                        cell(
                            textFieldWithHistoryWithBrowseButton(
                                project = project,
                                browseDialogTitle = "Select Mise Executable",
                                fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false),
                                historyProvider = { listOf("/opt/homebrew/bin/mise").distinct() },
                            ).apply {
                                setTextAndAddToHistory(service.state.executablePath)
                                setTextFieldPreferredWidth(50)
                            }
                        ).comment(
                            """
                            Specify the path to the mise executable.</br>
                            Not installed? Visit the <a href='https://mise.jdx.dev/installing-mise.html'>mise installation</a>
                            """.trimIndent()
                        ).resizableColumn()
                    }

                    group("Environments") {
                        row {
                            cell(myMiseDirEnvCb).comment(
                                "Load environment variables from mise configuration file(s)",
                            )
                        }
                        row("Config Environment:") {
                            cell(myMiseConfigEnvironmentTf)
                                .comment(
                                    """
                                    Specify the mise configuration environment to use (leave empty for default) <br/>
                                    <a href='https://mise.jdx.dev/configuration/environments.html'>Learn more about mise configuration environments</a>
                                    """.trimIndent(),
                                ).columns(COLUMNS_LARGE)
                                .focused()
                                .resizableColumn()
                        }.enabledIf(myMiseDirEnvCb.selected)
                    }
                },
            )
        }
    }

    override fun isModified(): Boolean {
        val service = MiseSettings.getService(project)
        return myMiseDirEnvCb.isSelected != service.state.useMiseDirEnv ||
                myMiseConfigEnvironmentTf.text != service.state.miseConfigEnvironment
    }

    override fun apply() {
        if (isModified) {
            val service = MiseSettings.getService(project)
            service.state.useMiseDirEnv = myMiseDirEnvCb.isSelected
            service.state.miseConfigEnvironment = myMiseConfigEnvironmentTf.text
        }
    }

    override fun getId(): String = MiseConfigurable::class.java.name
}
