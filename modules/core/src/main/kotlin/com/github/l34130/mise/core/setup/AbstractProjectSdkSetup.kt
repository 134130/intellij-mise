package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

abstract class AbstractProjectSdkSetup :
    DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { configureSdk(it, true) }
    }

    abstract fun getDevToolName(project: Project): MiseDevToolName

    open fun getSettingsId(project: Project): String = javaClass.name

    open fun getSettingsDisplayName(project: Project): String = getDevToolName(project).canonicalName()

    open fun defaultAutoConfigure(project: Project): Boolean = false

    open fun defaultAutoInstall(project: Project): Boolean = false

    fun isAutoConfigureEnabled(project: Project): Boolean {
        val settings = project.service<MiseProjectSettings>()
        val effective =
            settings.effectiveSdkSetupOption(
                id = getSettingsId(project),
                defaultAutoInstall = defaultAutoInstall(project),
                defaultAutoConfigure = defaultAutoConfigure(project),
            )
        return effective.autoConfigure
    }

    fun isAutoInstallEnabled(project: Project): Boolean {
        val settings = project.service<MiseProjectSettings>()
        val effective =
            settings.effectiveSdkSetupOption(
                id = getSettingsId(project),
                defaultAutoInstall = defaultAutoInstall(project),
                defaultAutoConfigure = defaultAutoConfigure(project),
            )
        return effective.autoInstall
    }

    protected abstract fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus

    protected abstract fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    )

    open fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T>? = null

    fun configureSdk(
        project: Project,
        isUserInteraction: Boolean,
    ) {
        // Delegates to a coordinator so providers only implement the SDK-specific hooks.
        coordinator.run(project, this, isUserInteraction)
    }

    internal fun checkSdkStatusInternal(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus = checkSdkStatus(tool, project)

    internal fun applySdkConfigurationInternal(
        tool: MiseDevTool,
        project: Project,
    ) = applySdkConfiguration(tool, project)

    internal fun configurableClass(): KClass<out Configurable>? = getSettingsConfigurableClass<Configurable>()

    sealed interface SdkLocation {
        data object Project : SdkLocation

        data class Module(val name: String) : SdkLocation

        data object Setting : SdkLocation

        data class Custom(val label: String) : SdkLocation
    }

    sealed interface SdkStatus {
        data class NeedsUpdate(
            val currentSdkVersion: String?,
            val currentSdkLocation: SdkLocation? = null,
            val configureAction: ((Boolean) -> Unit)? = null,
        ) : SdkStatus

        data class MultipleNeedsUpdate(
            val updates: List<NeedsUpdate>,
        ) : SdkStatus

        object UpToDate : SdkStatus
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<AbstractProjectSdkSetup>("com.github.l34130.mise.miseSdkSetup")
        private val coordinator = SdkSetupCoordinator()

        fun runAll(project: Project, isUserInteraction: Boolean) {
            EP_NAME.extensionList.forEach { provider ->
                provider.configureSdk(project, isUserInteraction)
            }
        }
    }
}
