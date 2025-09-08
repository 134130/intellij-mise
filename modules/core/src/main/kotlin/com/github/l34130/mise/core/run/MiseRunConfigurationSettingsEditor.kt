package com.github.l34130.mise.core.run

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import org.jdom.Element
import javax.swing.JComponent

private val USER_DATA_KEY = Key<MiseRunConfigurationState>("Mise Run Settings")

class MiseRunConfigurationSettingsEditor<T : RunConfigurationBase<*>> : SettingsEditor<T>() {
    private var myConfigEnvironmentStrategy: AtomicProperty<ConfigEnvironmentStrategy> =
        AtomicProperty(ConfigEnvironmentStrategy.USE_PROJECT_SETTINGS)
    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise:")
    private val myMiseConfigEnvironmentTf = JBTextField()

    private lateinit var p: DialogPanel

    override fun createEditor(): JComponent {
        p =
            panel {
                row {
                    cell(myMiseDirEnvCb)
                    contextHelp(
                        description = "If unchecked, Mise environment variables aren't applied, regardless of project settings.",
                        title = "Enables Mise for this run configuration.",
                    )
                }

                indent {
                    buttonsGroup {
                        row {
                            radioButton("Use project settings", ConfigEnvironmentStrategy.USE_PROJECT_SETTINGS)
                                .comment("Apply the Mise settings defined in the project")
                        }
                        row {
                            radioButton("Override project settings", ConfigEnvironmentStrategy.OVERRIDE_PROJECT_SETTINGS)
                                .comment(
                                    """
                                    Use a specific Mise config for this run configuration
                                    """.trimIndent(),
                                )
                        }
                    }.bind({ myConfigEnvironmentStrategy.get() }, { myConfigEnvironmentStrategy.set(it) })

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
                    }.enabledIf(
                        object : ComponentPredicate() {
                            override fun addListener(listener: (Boolean) -> Unit) {
                                myConfigEnvironmentStrategy.afterChange { listener(invoke()) }
                            }

                            override fun invoke(): Boolean =
                                myConfigEnvironmentStrategy.get() == ConfigEnvironmentStrategy.OVERRIDE_PROJECT_SETTINGS
                        },
                    )
                }.enabledIf(myMiseDirEnvCb.selected)
            }
        return p
    }

    // Write to persistence from the UI
    override fun applyEditorTo(config: T) {
        p.apply() // Call the registered callbacks to update the model

        config.putCopyableUserData(
            USER_DATA_KEY,
            MiseRunConfigurationState(
                configEnvironmentStrategy = myConfigEnvironmentStrategy.get(),
                useMiseDirEnv = myMiseDirEnvCb.isSelected,
                miseConfigEnvironment = myMiseConfigEnvironmentTf.text,
            ),
        )
    }

    // Read from persistence to the UI
    override fun resetEditorFrom(config: T) {
        val runConfigurationState = config.getCopyableUserData(USER_DATA_KEY) ?: return

        myConfigEnvironmentStrategy.set(runConfigurationState.configEnvironmentStrategy)
        myMiseDirEnvCb.isSelected = runConfigurationState.useMiseDirEnv
        myMiseConfigEnvironmentTf.text = runConfigurationState.miseConfigEnvironment

        p.reset() // Call the registered callbacks to update the UI
    }

    companion object {
        const val EDITOR_TITLE: String = "Mise"
        val SERIALIZATION_ID: String = MiseRunConfigurationSettingsEditor::class.java.name

        fun readExternal(
            runConfiguration: RunConfigurationBase<*>,
            element: Element,
        ) {
            val configEnvironmentStrategy =
                element.getAttributeValue("myConfigEnvironmentStrategy")?.let {
                    ConfigEnvironmentStrategy.from(it)
                } ?: MiseRunConfigurationState().configEnvironmentStrategy
            val miseDirEnvCb = element.getAttributeValue("myMiseDirEnvCb")?.toBoolean() ?: false
            val miseConfigEnvironment = element.getAttributeValue("myMiseConfigEnvironmentTf") ?: ""

            val state =
                MiseRunConfigurationState(
                    configEnvironmentStrategy = configEnvironmentStrategy,
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

            element.setAttribute("myConfigEnvironmentStrategy", userData.configEnvironmentStrategy.value)
            element.setAttribute("myMiseDirEnvCb", userData.useMiseDirEnv.toString())
            element.setAttribute("myMiseConfigEnvironmentTf", userData.miseConfigEnvironment)
        }

        fun getMiseRunConfigurationState(configuration: RunConfigurationBase<*>): MiseRunConfigurationState? =
            configuration.getCopyableUserData(USER_DATA_KEY)
    }
}
