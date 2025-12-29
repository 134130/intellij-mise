package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.MiseConfigFileResolver
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.TerminalUtils
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.application
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

abstract class AbstractProjectSdkSetup :
    DumbAwareAction(),
    ProjectActivity,
    DumbAware {
    final override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { configureSdk(it, true) }
    }

    override suspend fun execute(project: Project) {
        configureSdk(project, false)
    }

    abstract fun getDevToolName(project: Project): MiseDevToolName

    protected abstract fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus

    protected abstract fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult

    abstract fun <T : Configurable> getConfigurableClass(): KClass<out T>?

    private fun configureSdk(
        project: Project,
        isUserInteraction: Boolean,
    ) {
        application.executeOnPooledThread {
            val devToolName = getDevToolName(project)
            val miseNotificationService = project.service<MiseNotificationService>()

            val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
            
            // Skip automatic SDK configuration if the project doesn't have mise config files
            if (!isUserInteraction && !hasMiseConfigFiles(project, configEnvironment)) {
                return@executeOnPooledThread
            }
            val toolsResult =
                MiseCommandLineHelper.getDevTools(workDir = project.basePath, configEnvironment = configEnvironment)
            val tools =
                toolsResult.fold(
                    onSuccess = { tools -> tools[devToolName] },
                    onFailure = {
                        if (it !is MiseCommandLineNotFoundException) {
                            MiseNotificationServiceUtils.notifyException("Failed to load dev tools", it, project)
                        }
                        emptyList()
                    },
                )

            if (tools.isNullOrEmpty() || tools.size > 1) {
                if (!isUserInteraction) return@executeOnPooledThread

                val noOrMultiple =
                    if (tools.isNullOrEmpty()) {
                        "No"
                    } else {
                        "Multiple"
                    }

                miseNotificationService.warn(
                    "$noOrMultiple dev tools configuration for ${devToolName.canonicalName()} found",
                    "Check your Mise configuration or configure it manually",
                ) {
                    NotificationAction.createSimple("Configure") {
                        val configurableClass = getConfigurableClass<Configurable>()
                        if (configurableClass != null) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass.javaObjectType)
                        } else {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project)
                        }
                    }
                }

                return@executeOnPooledThread
            }

            val tool = tools.first()

            if (!tool.installed) {
                miseNotificationService.warn(
                    "$devToolName@${tool.shimsVersion()} is not installed",
                    "Run `mise install` command to install the tool",
                ) {
                    NotificationAction.createSimple("Run `mise install`") {
                        TerminalUtils.executeCommand(
                            project = project,
                            command = "mise install",
                            tabName = "mise install",
                        )
                    }
                }
                return@executeOnPooledThread
            }

            try {
                val status = checkSdkStatus(tool, project)
                when (status) {
                    is SdkStatus.NeedsUpdate -> {
                        val title =
                            if (status.currentSdkVersion == null) {
                                "${devToolName.canonicalName()} is not configured"
                            } else {
                                "${devToolName.canonicalName()} is misconfigured as '${devToolName.value}@${status.currentSdkVersion}'"
                            }

                        val applyAction = {
                            applySdkConfiguration(tool, project)
                            miseNotificationService.info(
                                "${devToolName.canonicalName()} is configured to '${devToolName.value}@${tool.shimsVersion()}'",
                                FileUtil.getLocationRelativeToUserHome(status.requestedInstallPath),
                            )
                        }

                        if (isUserInteraction) {
                            applyAction()
                        } else {
                            miseNotificationService.info(title, "Can configure as '${devToolName.value}@${tool.shimsVersion()}'") {
                                NotificationAction.createSimpleExpiring("Apply", applyAction)
                            }
                        }
                    }
                    SdkStatus.UpToDate -> {
                        if (!isUserInteraction) return@executeOnPooledThread

                        miseNotificationService.info(
                            "${devToolName.canonicalName()} is up to date",
                            "Currently using ${devToolName.value}@${tool.shimsVersion()}",
                        )
                    }
                }
            } catch (e: Throwable) {
                miseNotificationService.error(
                    "Failed to set ${devToolName.canonicalName()} to ${devToolName.value}@${tool.shimsVersion()}",
                    e.message ?: e.javaClass.simpleName,
                )
            }
        }
    }

    private fun hasMiseConfigFiles(
        project: Project,
        configEnvironment: String?,
    ): Boolean {
        val basePath = project.basePath ?: return false
        val baseDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(basePath)
            ?: return false
        
        return runBlocking {
            val configFiles = project.service<MiseConfigFileResolver>()
                .resolveConfigFiles(baseDir, refresh = false, configEnvironment = configEnvironment)
            configFiles.isNotEmpty()
        }
    }

    protected sealed interface SdkStatus {
        data class NeedsUpdate(
            val currentSdkVersion: String?,
            val requestedInstallPath: String,
        ) : SdkStatus

        object UpToDate : SdkStatus
    }

    protected data class ApplySdkResult(
        val sdkName: String,
        val sdkVersion: String,
        val sdkPath: String,
    )
}
