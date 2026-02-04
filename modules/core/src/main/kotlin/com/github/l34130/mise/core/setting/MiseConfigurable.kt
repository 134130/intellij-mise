package com.github.l34130.mise.core.setting

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
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
    // Single executable path field with auto-detection (Git-style)
    // Use ExtendableTextField which supports emptyText
    private val myMiseExecutableTf = TextFieldWithBrowseButton(ExtendableTextField())

    // Checkbox to determine if path is saved project-only
    private val myProjectOnlyCb = JBCheckBox("Set this path only for the current project")

    // Other project settings
    private val myMiseDirEnvCb = JBCheckBox("Use environment variables from mise")
    private val myMiseConfigEnvironmentTf = JBTextField()

    // Granular injection controls
    private val myMiseRunConfigsCb = JBCheckBox("Use in run configurations")
    private val myMiseVcsCb = JBCheckBox("Enable VCS Integration")
    private val myMiseDatabaseCb = JBCheckBox("Use in database authentication")
    private val myMiseNxCb = JBCheckBox("Use in Nx commands")
    private val myMiseAllCommandLinesCb = JBCheckBox("Use in all other command line execution")
    // Track per-provider rows for change detection and apply.
    private val sdkSetupRows = mutableListOf<SdkSetupRow>()

    override fun getDisplayName(): String = "Mise Settings"

    override fun createComponent(): JComponent {
        val applicationSettings = application.service<MiseApplicationSettings>()
        val projectSettings = project.service<MiseProjectSettings>()
        val executableManager = project.service<MiseExecutableManager>()

        // Determine which setting to load: project or app
        val projectPath = projectSettings.state.executablePath
        val appPath = applicationSettings.state.executablePath

        // Load current configuration
        if (projectPath.isNotBlank()) {
            // Project-level setting exists
            myMiseExecutableTf.text = projectPath
            myProjectOnlyCb.isSelected = true
        } else if (appPath.isNotBlank()) {
            // App-level setting exists
            myMiseExecutableTf.text = appPath
            myProjectOnlyCb.isSelected = false
        } else {
            // No setting - show empty
            myMiseExecutableTf.text = ""
            myProjectOnlyCb.isSelected = false
        }

        myMiseDirEnvCb.isSelected = projectSettings.state.useMiseDirEnv
        myMiseConfigEnvironmentTf.text = projectSettings.state.miseConfigEnvironment
        myMiseRunConfigsCb.isSelected = projectSettings.state.useMiseInRunConfigurations
        myMiseVcsCb.isSelected = projectSettings.state.useMiseVcsIntegration
        myMiseDatabaseCb.isSelected = projectSettings.state.useMiseInDatabaseAuthentication
        myMiseNxCb.isSelected = projectSettings.state.useMiseInNxCommands
        myMiseAllCommandLinesCb.isSelected = projectSettings.state.useMiseInAllCommandLines
        sdkSetupRows.clear()

        // Set placeholder text for auto-detected path
        val autoDetectedInfo = executableManager.getAutoDetectedExecutableInfo()
        val autoDetectedPath = autoDetectedInfo.path
        val autoDetectedVersion = autoDetectedInfo.version?.toString()
        val autoDetectedLabel = if (autoDetectedVersion != null) {
            "Auto-detected: $autoDetectedPath (version: $autoDetectedVersion)"
        } else {
            "Auto-detected: $autoDetectedPath"
        }
        (myMiseExecutableTf.textField as ExtendableTextField).emptyText.text = autoDetectedLabel

        // Configure file chooser to open at current or auto-detected location
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select Mise Executable")

        // Set up custom browse listener that opens at the appropriate location
        val browseListener = object : TextBrowseFolderListener(fileChooserDescriptor, project) {
            override fun getInitialFile(): VirtualFile? {
                // If field has value, use it; otherwise use auto-detected
                val currentPath = myMiseExecutableTf.text.takeIf { it.isNotBlank() } ?: autoDetectedPath
                return if (currentPath.isNotBlank()) {
                    val file = LocalFileSystem.getInstance().findFileByPath(currentPath)
                    file?.parent
                } else {
                    null
                }
            }
        }
        myMiseExecutableTf.addBrowseFolderListener(browseListener)

        return panel {
            group("Mise Executable", indent = false) {
                row("Path to Mise executable:") {
                    cell(myMiseExecutableTf)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment(
                            """
                            Leave empty to auto-detect from PATH or common locations (recommended).</br>
                            For WSL: <code>\\wsl.localhost\DistroName\path\to\mise</code></br>
                            Not installed? Visit the <a href='https://mise.jdx.dev/installing-mise.html'>mise installation</a>
                            """.trimIndent()
                        )
                }
                row {
                    cell(myProjectOnlyCb)
                        .comment("When checked, the path is saved for this project only. Otherwise, it applies to all projects.")
                }
            }

            group("Project Settings", indent = false) {
                panel {
                    indent {
                        row {
                            cell(myMiseDirEnvCb)
                                .resizableColumn()
                                .align(AlignX.FILL)
                                .comment("Master toggle for all mise environment variable injection")
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

                            row {
                                cell(myMiseRunConfigsCb)
                                    .resizableColumn()
                                    .comment("Apply to run/debug configurations (can be overridden per configuration)")
                            }.enabledIf(myMiseDirEnvCb.selected)

                            row {
                                cell(myMiseVcsCb)
                                    .resizableColumn()
                                    .comment("Enable mise environment variables and tools for VCS operations")
                            }.enabledIf(myMiseDirEnvCb.selected)

                            row {
                                cell(myMiseDatabaseCb)
                                    .resizableColumn()
                                    .comment("Use mise environment variables for database authentication")
                            }.enabledIf(myMiseDirEnvCb.selected)

                            // Conditional: Only show if Nx plugin is installed
                            if (PluginManager.getLoadedPlugins().any { it.pluginId.idString == "dev.nx.console" }) {
                                row {
                                    cell(myMiseNxCb)
                                        .resizableColumn()
                                        .comment("Inject environment variables when running Nx commands")
                                }.enabledIf(myMiseDirEnvCb.selected)
                            }

                            row {
                                cell(myMiseAllCommandLinesCb)
                                    .resizableColumn()
                                    .comment("Inject into all other command line execution (terminal, external tools, etc.)")
                            }.enabledIf(myMiseDirEnvCb.selected)
                        }
                    }
                }
            }

            group("SDK Setup", indent = false) {
                val sdkSetupProviders =
                    AbstractProjectSdkSetup.EP_NAME.extensionList
                        .map { provider ->
                            val displayName = provider.getSettingsDisplayName(project).ifBlank { provider.javaClass.simpleName }
                            SdkSetupProvider(provider, displayName)
                        }
                        // Stable ordering makes it easier to scan settings and avoids UI jitter.
                        .sortedBy { it.displayName.lowercase() }

                if (sdkSetupProviders.isEmpty()) {
                    row {
                        label("No SDK setup providers are registered for this IDE.")
                    }
                } else {
                    panel {
                        indent {
                            sdkSetupProviders.forEach { entry ->
                                val provider = entry.provider
                                val id = provider.getSettingsId(project)
                                val defaultAutoInstall = provider.defaultAutoInstall(project)
                                val defaultAutoConfigure = provider.defaultAutoConfigure(project)
                                val effectiveOption =
                                    projectSettings.effectiveSdkSetupOption(
                                        id = id,
                                        defaultAutoInstall = defaultAutoInstall,
                                        defaultAutoConfigure = defaultAutoConfigure,
                                    )

                                val autoInstallCb = JBCheckBox("Auto install").apply {
                                    isSelected = effectiveOption.autoInstall
                                }
                                val autoConfigureCb = JBCheckBox("Auto configure").apply {
                                    isSelected = effectiveOption.autoConfigure
                                }

                                sdkSetupRows.add(
                                    SdkSetupRow(
                                        id = id,
                                        autoInstall = autoInstallCb,
                                        autoConfigure = autoConfigureCb,
                                        defaultAutoInstall = defaultAutoInstall,
                                        defaultAutoConfigure = defaultAutoConfigure,
                                    ),
                                )

                                row(entry.displayName) {
                                    cell(autoInstallCb)
                                    cell(autoConfigureCb)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val applicationSettings = application.service<MiseApplicationSettings>()
        val projectSettings = project.service<MiseProjectSettings>()

        // Check if path changed
        val currentPath = myMiseExecutableTf.text.trim()
        val projectPath = projectSettings.state.executablePath
        val appPath = applicationSettings.state.executablePath

        val pathChanged = if (myProjectOnlyCb.isSelected) {
            // Should be in project settings
            currentPath != projectPath || appPath.isNotBlank()
        } else {
            // Should be in app settings
            currentPath != appPath || projectPath.isNotBlank()
        }

        return pathChanged ||
            myMiseDirEnvCb.isSelected != projectSettings.state.useMiseDirEnv ||
            myMiseConfigEnvironmentTf.text != projectSettings.state.miseConfigEnvironment ||
            myMiseRunConfigsCb.isSelected != projectSettings.state.useMiseInRunConfigurations ||
            myMiseVcsCb.isSelected != projectSettings.state.useMiseVcsIntegration ||
            myMiseDatabaseCb.isSelected != projectSettings.state.useMiseInDatabaseAuthentication ||
            myMiseNxCb.isSelected != projectSettings.state.useMiseInNxCommands ||
            myMiseAllCommandLinesCb.isSelected != projectSettings.state.useMiseInAllCommandLines ||
            sdkSetupRows.any { row ->
                val effectiveOption =
                    projectSettings.effectiveSdkSetupOption(
                        id = row.id,
                        defaultAutoInstall = row.defaultAutoInstall,
                        defaultAutoConfigure = row.defaultAutoConfigure,
                    )
                row.autoInstall.isSelected != effectiveOption.autoInstall ||
                    row.autoConfigure.isSelected != effectiveOption.autoConfigure
            }
    }

    override fun apply() {
        if (isModified) {
            val applicationSettings = application.service<MiseApplicationSettings>()
            val projectSettings = project.service<MiseProjectSettings>()

            val pathValue = myMiseExecutableTf.text.trim()

            if (myProjectOnlyCb.isSelected) {
                // Save to project settings only
                projectSettings.state.executablePath = pathValue
                // Clear app setting if it was there
                applicationSettings.state.executablePath = ""
            } else {
                // Save to app settings
                applicationSettings.state.executablePath = pathValue
                // Clear project setting
                projectSettings.state.executablePath = ""
            }

            projectSettings.state.useMiseDirEnv = myMiseDirEnvCb.isSelected
            projectSettings.state.miseConfigEnvironment = myMiseConfigEnvironmentTf.text
            projectSettings.state.useMiseInRunConfigurations = myMiseRunConfigsCb.isSelected
            projectSettings.state.useMiseVcsIntegration = myMiseVcsCb.isSelected
            projectSettings.state.useMiseInDatabaseAuthentication = myMiseDatabaseCb.isSelected
            projectSettings.state.useMiseInNxCommands = myMiseNxCb.isSelected
            projectSettings.state.useMiseInAllCommandLines = myMiseAllCommandLinesCb.isSelected
            sdkSetupRows.forEach { row ->
                projectSettings.upsertSdkSetupOption(
                    id = row.id,
                    autoInstall = row.autoInstall.isSelected,
                    autoConfigure = row.autoConfigure.isSelected,
                )
            }

            // Notify listeners that settings have changed
            MiseProjectEventListener.broadcast(
                project,
                MiseProjectEvent(MiseProjectEvent.Kind.SETTINGS_CHANGED, "settings applied")
            )
        }
    }

    override fun getId(): String = MiseConfigurable::class.java.name

    private data class SdkSetupProvider(
        val provider: AbstractProjectSdkSetup,
        val displayName: String,
    )

    private data class SdkSetupRow(
        val id: String,
        val autoInstall: JBCheckBox,
        val autoConfigure: JBCheckBox,
        val defaultAutoInstall: Boolean,
        val defaultAutoConfigure: Boolean,
    )
}
