package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.intellij.diagnostic.AbstractMessage
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.PluginException
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.util.Consumer
import org.apache.commons.lang3.StringUtils
import java.awt.Component
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MiseErrorReportSubmitter : ErrorReportSubmitter() {
    override fun getReportActionText(): String = "Report to Author"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, "Submitting Error report") {
            override fun run(indicator: ProgressIndicator) {
                val appInfo = ApplicationInfoEx.getInstance()
                val applicationNamesInfo = ApplicationNamesInfo.getInstance()

                for (event in events) {
                    var throwable = event.throwable
                    val description = additionalInfo ?: ""
                    var attachments = listOf<Attachment>()

                    if (event.data is LogMessage) {
                        throwable = (event.data as AbstractMessage).throwable
                        attachments = (event.data as AbstractMessage).includedAttachments
                    }
                    if (throwable is PluginException && throwable.cause != null) {
                        // unwrap PluginManagerCore.createPluginException
                        throwable = throwable.cause
                    }

                    val title = "[${applicationNamesInfo.productName} ${appInfo.fullVersion}] ${
                        StringUtils.abbreviate(throwable.message, 80)
                    }"

                    val body =
                        buildString {
                            appendLine("### Environment")
                            appendLine("- IDE: ${applicationNamesInfo.productName} ${appInfo.fullVersion}")
                            appendLine("- Plugin: ${pluginDescriptor.name} ${pluginDescriptor.version}")
                            appendLine("- Mise: mise@${MiseCommandLine.getMiseVersion()}")
                            appendLine("- OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
                            appendLine("- Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")

                            appendLine()

                            appendLine("### Description")
                            appendLine("- $description")

                            appendLine()

                            appendLine("### Stacktrace")
                            appendLine("```")
                            appendLine(throwable.message)
                            appendLine(throwable.stackTrace.take(25).joinToString("\n"))
                            appendLine("```")

                            appendLine()

                            appendLine("### Attachments")
                            for (attachment in attachments) {
                                appendLine("- $attachment")
                            }
                        }

                    Desktop
                        .getDesktop()
                        .browse(
                            URI.create(
                                buildString {
                                    append(
                                        "https://github.com/134130/intellij-mise/issues/new?labels=bug,${applicationNamesInfo.productName}",
                                    )
                                    append("&title=${URLEncoder.encode(title, StandardCharsets.UTF_8)}")
                                    append("&body=${URLEncoder.encode(body, StandardCharsets.UTF_8)}")
                                },
                            ),
                        )
                }

                invokeLater {
                    MiseNotificationService.getInstance(project).info(
                        title = "Error Report Submitted",
                        htmlText = "Thank you for submitting your report!",
                    )

                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            }
        }.queue()

        return true
    }
}
