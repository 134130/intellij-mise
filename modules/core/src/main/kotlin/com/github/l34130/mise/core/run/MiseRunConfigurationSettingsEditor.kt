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
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import com.intellij.util.application
import org.jdom.Element
import javax.swing.JComponent

private val USER_DATA_KEY = Key<MiseRunConfigurationState>("Mise Run Settings")

class MiseRunConfigurationSettingsEditor<T : RunConfigurationBase<*>>(
    private val project: Project,
) : SettingsEditor<T>() {
    private val applicationState = application.service<MiseSettings>().state
    private val useApplicationWideMiseConfig = applicationState.useMiseDirEnv
    private val useApplicationWideMiseConfigPredicate = ComponentPredicate.fromValue(useApplicationWideMiseConfig)

    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise")
    private val myMiseConfigEnvironmentTf = JBTextField()

    override fun createEditor(): JComponent =
        panel {
            row {
                cell(myMiseDirEnvCb)
                    .comment("Load environment variables from mise configuration file(s)")
            }.enabledIf(useApplicationWideMiseConfigPredicate.not())
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
            }.enabledIf(myMiseDirEnvCb.selected.and(useApplicationWideMiseConfigPredicate.not()))
            row {
                icon(AllIcons.General.ShowWarning)
                label("Using the configuration in Settings / Tools / Mise Settings")
                    .bold()
            }.visibleIf(useApplicationWideMiseConfigPredicate)
        }

    // Write to persistence from the UI
    override fun applyEditorTo(config: T) {
        val projectState = application.service<MiseSettings>().state

        // When the project is configured to use the project-wide mise configuration
        if (projectState.useMiseDirEnv) {
            // Ignore applying the settings to the run configuration
            return
        }

        config.putCopyableUserData(
            USER_DATA_KEY,
            MiseRunConfigurationState(
                useMiseDirEnv = myMiseDirEnvCb.isSelected,
                miseConfigEnvironment = myMiseConfigEnvironmentTf.text,
            ),
        )
    }

    // Read from persistence to the UI
    override fun resetEditorFrom(config: T) {
        val runConfigurationState = config.getCopyableUserData(USER_DATA_KEY) ?: return

        val useMiseDirEnv: Boolean
        val miseConfigEnvironment: String

        when (useApplicationWideMiseConfig) {
            true -> {
                useMiseDirEnv = applicationState.useMiseDirEnv
                miseConfigEnvironment = applicationState.miseConfigEnvironment
            }
            false -> {
                useMiseDirEnv = runConfigurationState.useMiseDirEnv
                miseConfigEnvironment = runConfigurationState.miseConfigEnvironment
            }
        }

        myMiseDirEnvCb.isSelected = useMiseDirEnv
        myMiseConfigEnvironmentTf.text = miseConfigEnvironment
    }

    companion object {
        const val EDITOR_TITLE: String = "Mise"
        val SERIALIZATION_ID: String = MiseRunConfigurationSettingsEditor::class.java.name

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val miseDirEnvCb = element.getAttributeValue("myMiseDirEnvCb")?.toBoolean() ?: false
            val miseConfigEnvironment = element.getAttributeValue("myMiseConfigEnvironmentTf") ?: ""
            val state =
                MiseRunConfigurationState(
                    useMiseDirEnv = miseDirEnvCb,
                    miseConfigEnvironment = miseConfigEnvironment,
                )
            runConfiguration.putCopyableUserData(USER_DATA_KEY, state)
        }

        fun writeExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val userData = runConfiguration.getCopyableUserData(USER_DATA_KEY) ?: return

            element.setAttribute("myMiseDirEnvCb", userData.useMiseDirEnv.toString())
            element.setAttribute("myMiseConfigEnvironmentTf", userData.miseConfigEnvironment)
        }

        fun getMiseRunConfigurationState(configuration: RunConfigurationBase<*>): MiseRunConfigurationState? =
            configuration.getCopyableUserData(USER_DATA_KEY)
    }
}
