package com.github.l34130.mise.core.run

import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.jdom.Element
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

private val USER_DATA_KEY = Key<MiseRunConfigurationState>("Mise Run Settings")

class MiseRunConfigurationSettingsEditor<T : RunConfigurationBase<*>>(
    private val project: Project,
) : SettingsEditor<T>() {
    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise")
    private val myMiseProfileTf = JBTextField()

    override fun createEditor(): JComponent {
        val projectState = MiseSettings.getService(project).state
        if (projectState.useMiseDirEnv) {
            myMiseDirEnvCb.isEnabled = false
            myMiseProfileTf.isEnabled = false
        }

        myMiseDirEnvCb.addChangeListener {
            myMiseProfileTf.isEnabled = myMiseDirEnvCb.isSelected
        }

        val isOverridden = projectState.useMiseDirEnv

        return JPanel(BorderLayout()).apply {
            add(
                panel {
                    row {
                        cell(myMiseDirEnvCb)
                            .comment("Load environment variables from mise configuration file(s)")
                            .enabled(!isOverridden)
                    }
                    row("Profile: ") {
                        cell(myMiseProfileTf)
                            .comment(
                                "Specify the mise profile to use (leave empty for default)" +
                                    "<br/><a href='https://mise.jdx.dev/profiles.html#profiles'>Learn more about mise profiles</a>",
                            ).columns(COLUMNS_LARGE)
                            .focused()
                            .resizableColumn()
                            .enabled(!isOverridden)
                    }
                    row {
                        icon(AllIcons.General.ShowWarning)
                        label("Using the configuration in Settings / Tools / Mise Settings")
                            .visible(isOverridden)
                            .bold()
                    }
                },
            )
        }
    }

    override fun applyEditorTo(config: T) {
        config.putCopyableUserData(
            USER_DATA_KEY,
            MiseRunConfigurationState(
                useMiseDirEnv = myMiseDirEnvCb.isSelected,
                miseProfile = myMiseProfileTf.text,
            ),
        )
    }

    override fun resetEditorFrom(config: T) {
        val userData = config.getCopyableUserData(USER_DATA_KEY) ?: return

        val projectState = project.service<MiseSettings>().state
        val state = userData.mergeProjectState(projectState)

        myMiseDirEnvCb.isSelected = state.useMiseDirEnv
        myMiseProfileTf.text = state.miseProfile
        if (!myMiseDirEnvCb.isSelected) {
            myMiseProfileTf.isEnabled = false
        }

        if (projectState.useMiseDirEnv) {
            myMiseDirEnvCb.isEnabled = false
            myMiseProfileTf.isEnabled = false
        }
    }

    companion object {
        const val EDITOR_TITLE: String = "Mise"
        val SERIALIZATION_ID: String = MiseRunConfigurationSettingsEditor::class.java.name

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val miseDirEnvCb = element.getAttributeValue("myMiseDirEnvCb")?.toBoolean() ?: false
            val miseProfile = element.getAttributeValue("myMiseProfileTf") ?: ""
            val state =
                MiseRunConfigurationState(
                    useMiseDirEnv = miseDirEnvCb,
                    miseProfile = miseProfile,
                )
            runConfiguration.putCopyableUserData(USER_DATA_KEY, state)
        }

        fun writeExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val userData = runConfiguration.getCopyableUserData(USER_DATA_KEY) ?: return

            element.setAttribute("myMiseDirEnvCb", userData.useMiseDirEnv.toString())
            element.setAttribute("myMiseProfileTf", userData.miseProfile)
        }

        fun getMiseRunConfigurationState(configuration: RunConfigurationBase<*>): MiseRunConfigurationState? =
            configuration.getCopyableUserData(USER_DATA_KEY)
    }
}
