package com.github.l34130.mise.settings.ui

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

class RunConfigurationSettingsPanel : JPanel() {
    val enableMiseCheckBox = JBCheckBox("Enable mise")

    init {
        val p = panel {
            row {
                cell(enableMiseCheckBox).comment(
                    "Load environment variables from mise configuration file(s)",
                )
            }
        }

        layout = BorderLayout()
        add(p)
    }

    var state: MiseSettings.State
        get() = MiseSettings.State(isMiseEnabled = enableMiseCheckBox.isSelected)
        set(value) {
            enableMiseCheckBox.isSelected = value.isMiseEnabled
        }
}
