package com.github.l34130.mise.run

import com.github.l34130.mise.settings.MiseSettings
import com.intellij.execution.configurations.RunConfigurationBase
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
        val service = MiseSettings.getService(project)
        myMiseDirEnvCb.isSelected = service.state.useMiseDirEnv
        myMiseProfileTf.text = service.state.miseProfile

        return JPanel(BorderLayout()).apply {
            add(
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

        myMiseDirEnvCb.isSelected = userData.useMiseDirEnv
        myMiseProfileTf.text = userData.miseProfile
    }

    companion object {
        const val EDITOR_TITLE: String = "Mise"
        val SERIALIZATION_ID: String = Companion::class.qualifiedName!!

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
