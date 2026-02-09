package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.intellij.notification.NotificationAction
import com.intellij.openapi.project.Project
import com.intellij.xml.util.XmlStringUtil

// Applies SDK configuration and emits notifications for sync decisions.
internal class SdkConfigureCoordinator(
    private val settingsUpdater: SdkSetupSettingsUpdater = SdkSetupSettingsUpdater(),
) {
    fun applyIfNeeded(
        project: Project,
        provider: AbstractProjectSdkSetup,
        tool: MiseDevTool,
        devToolName: MiseDevToolName,
        displayName: String,
        isUserInteraction: Boolean,
        autoConfigureEnabled: Boolean,
        notificationService: MiseNotificationService,
    ) {
        try {
            val status = provider.checkSdkStatusInternal(tool, project)
            when (status) {
                is AbstractProjectSdkSetup.SdkStatus.NeedsUpdate ->
                    handleNeedsUpdate(
                        status,
                        project,
                        provider,
                        tool,
                        devToolName,
                        displayName,
                        isUserInteraction,
                        autoConfigureEnabled,
                        notificationService,
                    )
                is AbstractProjectSdkSetup.SdkStatus.MultipleNeedsUpdate -> {
                    status.updates.forEach { update ->
                        handleNeedsUpdate(
                            update,
                            project,
                            provider,
                            tool,
                            devToolName,
                            displayName,
                            isUserInteraction,
                            autoConfigureEnabled,
                            notificationService,
                        )
                    }
                }
                AbstractProjectSdkSetup.SdkStatus.UpToDate -> {
                    if (!isUserInteraction) return

                    notificationService.info(
                        "${devToolName.canonicalName()} is up to date",
                        "Currently using ${escapeHtml(devToolName.value)}@${escapeHtml(tool.displayVersion)}",
                    )
                }
            }
        } catch (e: Throwable) {
            notificationService.error(
                "Failed to set ${devToolName.canonicalName()} to ${escapeHtml(devToolName.value)}@${escapeHtml(tool.displayVersion)}",
                e.message ?: e.javaClass.simpleName,
            )
        }
    }
    private fun handleNeedsUpdate(
        status: AbstractProjectSdkSetup.SdkStatus.NeedsUpdate,
        project: Project,
        provider: AbstractProjectSdkSetup,
        tool: MiseDevTool,
        devToolName: MiseDevToolName,
        displayName: String,
        isUserInteraction: Boolean,
        autoConfigureEnabled: Boolean,
        notificationService: MiseNotificationService,
    ) {
        val titleQualifier = formatTitleQualifier(status.currentSdkLocation)
        val titleSuffix = titleQualifier?.let { " ($it)" }.orEmpty()
        val title =
            if (status.currentSdkVersion == null) {
                "${devToolName.canonicalName()} Not Configured$titleSuffix"
            } else {
                "${devToolName.canonicalName()} Version Mismatch$titleSuffix"
            }

        val description =
            if (status.currentSdkVersion == null) {
                "Configure as '${escapeHtml(devToolName.value)}@${escapeHtml(tool.displayVersion)}'"
            } else {
                val currentLabel = escapeHtml(formatLocationLabel(status.currentSdkLocation))
                val currentVersion = escapeHtml(status.currentSdkVersion)
                val displayVersionWithResolved = escapeHtml(tool.displayVersionWithResolved)
                val toolLabel = escapeHtml(devToolName.canonicalName())
                buildString {
                    append("$currentLabel: $currentVersion <br/>")
                    append("Mise: <b>$toolLabel $displayVersionWithResolved</b>")
                }
            }

        val configureAction = status.configureAction ?: { provider.applySdkConfigurationInternal(tool, project) }
        val applyAction: (Boolean) -> Unit = { isAuto ->
            configureAction(isAuto)
            if (isAuto) {
                notificationService.info(
                    "Auto-configured $displayName",
                    "Now using ${escapeHtml(devToolName.value)}@${escapeHtml(tool.displayVersionWithResolved)}",
                )
            } else {
                notificationService.info(
                    "${devToolName.canonicalName()} is configured to ${escapeHtml(tool.displayVersionWithResolved)}",
                    ""
                )
            }
        }

        when {
            // Auto-apply configuration in three cases:
            // 1. User manually triggered the action (isUserInteraction = true)
            isUserInteraction -> applyAction(false)
            // 2. Automatic startup AND SDK is not configured yet (currentSdkVersion == null)
            //    This prevents the "SDK is required" warning banner in the IDE
            status.currentSdkVersion == null -> applyAction(true)
            // 3. The user has specifically enabled auto-configuration in the settings.
            autoConfigureEnabled -> applyAction(true)
            else -> {
                // For version mismatches on startup, show notification to avoid
                // unexpectedly changing existing configuration
                notificationService.info(title, description) { notification ->
                    notification.addAction(
                        NotificationAction.createSimpleExpiring("Configure now") {
                            applyAction(false)
                        }
                    )
                    notification.addAction(
                        NotificationAction.createSimpleExpiring("Always keep ${escapeHtml(displayName)} in sync") {
                            settingsUpdater.update(project, provider, autoConfigure = true)
                            applyAction(true)
                        }
                    )
                }
            }
        }
    }

    private fun formatTitleQualifier(location: AbstractProjectSdkSetup.SdkLocation?): String? {
        return when (location) {
            AbstractProjectSdkSetup.SdkLocation.Project -> null
            AbstractProjectSdkSetup.SdkLocation.Setting -> "Setting"
            is AbstractProjectSdkSetup.SdkLocation.Module -> "Module: ${location.name}"
            is AbstractProjectSdkSetup.SdkLocation.Custom -> location.label
            null -> null
        }
    }

    private fun formatLocationLabel(location: AbstractProjectSdkSetup.SdkLocation?): String {
        return when (location) {
            AbstractProjectSdkSetup.SdkLocation.Project, null -> "Project"
            AbstractProjectSdkSetup.SdkLocation.Setting -> "Setting"
            is AbstractProjectSdkSetup.SdkLocation.Module -> "Module: ${location.name}"
            is AbstractProjectSdkSetup.SdkLocation.Custom -> location.label
        }
    }

    private fun escapeHtml(value: String?): String = XmlStringUtil.escapeString(value ?: "")
}
