package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.util.RunWindowUtils
import com.intellij.notification.NotificationAction
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

// Handles install prompts and non-interactive installs for a specific tool.
internal class InstallCoordinator(
    private val settingsUpdater: SdkSetupSettingsUpdater = SdkSetupSettingsUpdater(),
) {
    fun ensureInstalled(
        project: Project,
        provider: AbstractProjectSdkSetup,
        tool: MiseDevTool,
        devToolName: MiseDevToolName,
        displayName: String,
        isUserInteraction: Boolean,
        autoInstallEnabled: Boolean,
        notificationService: MiseNotificationService,
        postInstall: () -> Unit,
    ): InstallOutcome {
        if (tool.installed) {
            return InstallOutcome.Installed
        }

        // Use a non-interactive, tool-scoped install to avoid running global installs.
        val installArgs = listOf("install", "--raw", "--yes", devToolName.value)
        val installTabName = "Mise install ${devToolName.value}"
        val installCommandLabel = "mise install --raw --yes ${devToolName.value}"
        val runInstall = {
            val installKey = autoInstallKey(project, provider, devToolName)
            // Guard against concurrent installs triggered by settings/event rechecks.
            if (autoInstallInFlight.add(installKey)) {
                RunWindowUtils.executeMiseCommand(
                    project = project,
                    args = installArgs,
                    tabName = installTabName,
                    onSuccess = postInstall,
                    onFinish = { autoInstallInFlight.remove(installKey) },
                )
                true
            } else {
                false
            }
        }

        if (autoInstallEnabled) {
            if (!isUserInteraction) {
                notificationService.info(
                    "Installing $displayName via mise",
                    "Running `$installCommandLabel` to satisfy ${devToolName.value}@${tool.displayVersion}",
                )
            }
            runInstall()
            return InstallOutcome.InstallStarted
        }

        notificationService.warn(
            "$devToolName@${tool.displayVersion} is not installed",
            "Run `$installCommandLabel` to install the tool",
        ) { notification ->
            notification.addAction(
                NotificationAction.createSimpleExpiring("Install now") {
                    runInstall()
                }
            )
            notification.addAction(
                NotificationAction.createSimpleExpiring("Always install missing $displayName") {
                    settingsUpdater.update(project, provider, autoInstall = true)
                    runInstall()
                }
            )
        }

        return InstallOutcome.NotInstalled
    }

    private fun autoInstallKey(
        project: Project,
        provider: AbstractProjectSdkSetup,
        devToolName: MiseDevToolName,
    ): String =
        "${project.locationHash}:${provider.getSettingsId(project)}:${devToolName.value}:install"

    companion object {
        private val autoInstallInFlight = ConcurrentHashMap.newKeySet<String>()
    }
}

internal sealed interface InstallOutcome {
    data object Installed : InstallOutcome

    data object InstallStarted : InstallOutcome

    data object NotInstalled : InstallOutcome
}
