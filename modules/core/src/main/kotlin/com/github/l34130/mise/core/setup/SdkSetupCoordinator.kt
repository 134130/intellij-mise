package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.cache.MiseCacheService
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.application
import java.util.concurrent.ConcurrentHashMap

// Orchestrates tool resolution, install, and SDK configuration for a provider.
internal class SdkSetupCoordinator(
    private val toolResolver: ToolResolutionService = ToolResolutionService(),
    private val installCoordinator: InstallCoordinator = InstallCoordinator(),
    private val configureCoordinator: SdkConfigureCoordinator = SdkConfigureCoordinator(),
) {
    fun run(
        project: Project,
        provider: AbstractProjectSdkSetup,
        isUserInteraction: Boolean,
    ) {
        val autoConfigureKey = if (isUserInteraction) null else AutoConfigureGuard.key(project, provider)
        if (autoConfigureKey != null && !AutoConfigureGuard.tryAcquire(autoConfigureKey)) {
            return
        }

        // SDK checks can run on startup and file changes; keep them off the EDT.
        application.executeOnPooledThread {
            try {
                val devToolName = provider.getDevToolName(project)
                val notificationService = project.service<MiseNotificationService>()
                val settings = project.service<MiseProjectSettings>().state
                val configEnvironment = settings.miseConfigEnvironment
                val displayName = provider.getSettingsDisplayName(project)
                val autoConfigureEnabled = provider.isAutoConfigureEnabled(project)
                val autoInstallEnabled = provider.isAutoInstallEnabled(project)

                when (val resolution = toolResolver.resolve(project, configEnvironment, devToolName)) {
                    ToolResolution.None -> {
                        if (isUserInteraction) {
                            notifyMissingToolConfig(project, provider, notificationService, devToolName, isMultiple = false)
                        }
                        return@executeOnPooledThread
                    }
                    ToolResolution.Multiple -> {
                        if (isUserInteraction) {
                            notifyMissingToolConfig(project, provider, notificationService, devToolName, isMultiple = true)
                        }
                        return@executeOnPooledThread
                    }
                    is ToolResolution.Single -> {
                        val tool = resolution.tool
                        val postInstall = {
                            project.service<MiseCacheService>().invalidateAllCommands()
                            if (isUserInteraction) {
                                provider.configureSdk(project, isUserInteraction = true)
                            } else {
                                AbstractProjectSdkSetup.runAll(project, isUserInteraction = false)
                            }
                        }

                        val installOutcome =
                            installCoordinator.ensureInstalled(
                                project = project,
                                provider = provider,
                                tool = tool,
                                devToolName = devToolName,
                                displayName = displayName,
                                isUserInteraction = isUserInteraction,
                                autoInstallEnabled = autoInstallEnabled,
                                notificationService = notificationService,
                                postInstall = postInstall,
                            )

                        if (installOutcome != InstallOutcome.Installed) {
                            return@executeOnPooledThread
                        }

                        configureCoordinator.applyIfNeeded(
                            project = project,
                            provider = provider,
                            tool = tool,
                            devToolName = devToolName,
                            displayName = displayName,
                            isUserInteraction = isUserInteraction,
                            autoConfigureEnabled = autoConfigureEnabled,
                            notificationService = notificationService,
                        )
                    }
                }
            } finally {
                if (autoConfigureKey != null) {
                    AutoConfigureGuard.release(autoConfigureKey)
                }
            }
        }
    }

    private fun notifyMissingToolConfig(
        project: Project,
        provider: AbstractProjectSdkSetup,
        notificationService: MiseNotificationService,
        devToolName: MiseDevToolName,
        isMultiple: Boolean,
    ) {
        val prefix = if (isMultiple) "Multiple" else "No"
        notificationService.warn(
            "$prefix dev tools configuration for ${devToolName.canonicalName()} found",
            "Check your Mise configuration or configure it manually",
            actionProvider = {
                NotificationAction.createSimple("Configure") {
                    val configurableClass = provider.configurableClass()
                    if (configurableClass != null) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass.javaObjectType)
                    } else {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project)
                    }
                }
            },
        )
    }
}

// Prevents duplicate auto-config runs when multiple events arrive together.
private object AutoConfigureGuard {
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    fun key(project: Project, provider: AbstractProjectSdkSetup): String =
        "${project.locationHash}:${provider.getSettingsId(project)}"

    fun tryAcquire(key: String): Boolean = inFlight.add(key)

    fun release(key: String) {
        inFlight.remove(key)
    }
}
