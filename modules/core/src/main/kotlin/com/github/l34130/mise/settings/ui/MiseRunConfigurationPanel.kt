package com.github.l34130.mise.settings.ui

import com.github.l34130.mise.settings.MiseState
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

class MiseRunConfigurationPanel : JPanel() {
    val enableMiseCheckBox = JBCheckBox("Use environment variables from mise")
    val miseProfileField = JBTextField()

    init {
        val p = panel {
            row {
                cell(enableMiseCheckBox).comment(
                    "Load environment variables from mise configuration file(s)",
                )
            }
            row("Profile:") {
                cell(miseProfileField)
                    .comment(
                        "mise profile to load environment variables (leave empty for default)" +
                                "<br/><a href='https://mise.jdx.dev/profiles.html#profiles'>Learn more about mise profiles</a>"
                    ).columns(COLUMNS_MEDIUM)
                     .focused()
                    .resizableColumn()
            }
        }

        layout = BorderLayout()
        add(p)
    }

    var state: MiseState
        get() =
            MiseState(
                isMiseEnabled = enableMiseCheckBox.isSelected,
            miseProfile = miseProfileField.text.trim()
        )
        set(value) {
            enableMiseCheckBox.isSelected = value.isMiseEnabled
            miseProfileField.text = value.miseProfile
        }

    companion object {
        private const val COLUMNS_MEDIUM = 35
    }
}