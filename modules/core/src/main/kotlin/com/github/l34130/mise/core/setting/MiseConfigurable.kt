package com.github.l34130.mise.core.setting

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithHistoryWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.intellij.util.application
import javax.swing.JComponent

class MiseConfigurable(
    private val project: Project,
) : SearchableConfigurable {
    private val myMiseExecutableTf =
        textFieldWithHistoryWithBrowseButton(
            project = project,
            fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false).withTitle("Select Mise Executable"),
            historyProvider = { listOf("/opt/homebrew/bin/mise").distinct() },
        )
    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise")
    private val myMiseConfigEnvironmentTf = JBTextField()

    override fun getDisplayName(): String = "Mise Settings"

    override fun createComponent(): JComponent {
        val applicationSettings = application.service<MiseApplicationSettings>()
        val projectSettings = project.service<MiseProjectSettings>()

        myMiseExecutableTf.setTextAndAddToHistory(applicationSettings.state.executablePath)
        myMiseDirEnvCb.isSelected = projectSettings.state.useMiseDirEnv
        myMiseConfigEnvironmentTf.text = projectSettings.state.miseConfigEnvironment

        return panel {
            group("Application Settings", indent = false) {
                row("Mise Executable:") {
                    cell(myMiseExecutableTf)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment(
                            """
                            Specify the path to the mise executable.</br>
                            Not installed? Visit the <a href='https://mise.jdx.dev/installing-mise.html'>mise installation</a>
                            """.trimIndent(),
                        )
                }
            }

            group("Project Settings", indent = false) {
                panel {
                    indent {
                        row {
                            cell(myMiseDirEnvCb)
                                .resizableColumn()
                                .align(AlignX.FILL)
                                .comment("Load environment variables from mise configuration file(s)")
                        }
                        indent {
                            row("Config Environment:") {
                                cell(myMiseConfigEnvironmentTf)
                                    .columns(COLUMNS_MEDIUM)
                                    .comment(
                                        """
                                        Specify the mise configuration environment to use (leave empty for default) <br/>
                                        <a href='https://mise.jdx.dev/configuration/environments.html'>Learn more about mise configuration environments</a>
                                        """.trimIndent(),
                                    )
                            }.enabledIf(myMiseDirEnvCb.selected)
                        }
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val applicationSettings = application.service<MiseApplicationSettings>()
        val projectSettings = project.service<MiseProjectSettings>()
        return myMiseExecutableTf.text != applicationSettings.state.executablePath ||
            myMiseDirEnvCb.isSelected != projectSettings.state.useMiseDirEnv ||
            myMiseConfigEnvironmentTf.text != projectSettings.state.miseConfigEnvironment
    }

    override fun apply() {
        if (isModified) {
            val applicationSettings = application.service<MiseApplicationSettings>()
            val projectSettings = project.service<MiseProjectSettings>()
            applicationSettings.state.executablePath = myMiseExecutableTf.text
            projectSettings.state.useMiseDirEnv = myMiseDirEnvCb.isSelected
            projectSettings.state.miseConfigEnvironment = myMiseConfigEnvironmentTf.text
        }
    }

    override fun getId(): String = MiseConfigurable::class.java.name
}
