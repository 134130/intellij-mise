package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.cache.MiseProjectEvent
import com.github.l34130.mise.core.cache.MiseProjectEventListener
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

// Centralizes SDK setup setting updates and broadcasts the change event.
internal class SdkSetupSettingsUpdater {
    fun update(
        project: Project,
        provider: AbstractProjectSdkSetup,
        autoInstall: Boolean? = null,
        autoConfigure: Boolean? = null,
    ) {
        val settings = project.service<MiseProjectSettings>()
        val id = provider.getSettingsId(project)
        val effective =
            settings.effectiveSdkSetupOption(
                id = id,
                defaultAutoInstall = provider.defaultAutoInstall(project),
                defaultAutoConfigure = provider.defaultAutoConfigure(project),
            )

        settings.upsertSdkSetupOption(
            id = id,
            autoInstall = autoInstall ?: effective.autoInstall,
            autoConfigure = autoConfigure ?: effective.autoConfigure,
        )
        MiseProjectEventListener.broadcast(
            project,
            MiseProjectEvent(MiseProjectEvent.Kind.SETTINGS_CHANGED, "sdk setup option updated")
        )
    }
}
