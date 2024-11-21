package com.github.l34130.mise.core.setting

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
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
    private val myMiseProfileTf = JBTextField()

    override fun getDisplayName(): String = "Mise Settings"

    override fun createComponent(): JComponent {
        val service = MiseSettings.getService(project)

        myMiseDirEnvCb.isSelected = service.state.useMiseDirEnv
        myMiseProfileTf.text = service.state.miseProfile

        return JPanel(BorderLayout()).apply {
            add(
                panel {
                    row {
                        label("Environments").bold()
                    }

                    row {
                        panel {
                            row {
                                cell(myMiseDirEnvCb).comment(
                                    "Load environment variables from mise configuration file(s)",
                                )
                            }
                            row("Profile: ") {
                                cell(myMiseProfileTf)
                                    .comment(
                                        "Specify the mise profile to use (leave empty for default)" +
                                                "<br/><a href='https://mise.jdx.dev/profiles.html#profiles'>Learn more about mise profiles</a>",
                                    ).columns(COLUMNS_LARGE)
                                    .focused()
                                    .resizableColumn()
                            }.enabledIf(myMiseDirEnvCb.selected)
                        }
                    }
                },
            )
        }
    }

    override fun isModified(): Boolean {
        val service = MiseSettings.getService(project)
        return myMiseDirEnvCb.isSelected != service.state.useMiseDirEnv ||
            myMiseProfileTf.text != service.state.miseProfile
    }

    override fun apply() {
        if (isModified) {
            val service = MiseSettings.getService(project)
            service.state.useMiseDirEnv = myMiseDirEnvCb.isSelected
            service.state.miseProfile = myMiseProfileTf.text
        }
    }

    override fun getId(): String = MiseConfigurable::class.java.name
}
