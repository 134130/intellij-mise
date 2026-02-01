package com.github.l34130.mise.core

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.notification.MiseNotificationService
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.invokeLater
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
import java.security.MessageDigest

class MiseErrorReportSubmitter : ErrorReportSubmitter() {
    override fun getReportActionText(): String = "Report to Author"

    @OptIn(ExperimentalStdlibApi::class)
    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(context) ?: return false

        object : Task.Backgroundable(project, "Submitting Error report") {
            override fun run(indicator: ProgressIndicator) {
                val appInfo = ApplicationInfoEx.getInstance()
                val applicationNamesInfo = ApplicationNamesInfo.getInstance()

                for (event in events) {
                    val description = additionalInfo ?: ""

                    val title = "[${applicationNamesInfo.productName} ${appInfo.fullVersion}] ${
                        StringUtils.abbreviate(event.throwableText, 80)
                    }"

                    val body =
                        buildString {
                            appendLine("### Description")
                            appendLine("- $description")

                            appendLine()

                            appendLine("### Environment")
                            appendLine("- IDE: ${applicationNamesInfo.productName} ${appInfo.fullVersion}")
                            appendLine("- Plugin: ${pluginDescriptor.name} ${pluginDescriptor.version}")
                            val miseVersion = try {
                                val cmdLine = MiseCommandLine(project, project.guessMiseProjectPath(), null)
                                val result = cmdLine.runRawCommandLine(listOf("version"))
                                result.getOrNull()?.trim() ?: "unknown"
                            } catch (e: Exception) {
                                "error: ${e.message}"
                            }
                            appendLine("- Mise: $miseVersion")
                            appendLine("- OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")

                            appendLine()

                            appendLine("### Stacktrace")
                            val stacktrace =
                                event.throwableText
                                    .split("\n")
                                    .filter { line -> EXCEPTION_EXCLUSIONS.none { it.containsMatchIn(line) } }

                            appendLine(
                                "Hash: `${MessageDigest.getInstance("MD5")
                                    .digest(stacktrace.joinToString("\n").toByteArray())
                                    .toHexString()}",
                            )

                            appendLine("```")
                            appendLine(stacktrace.take(25).joinToString("\n"))
                            appendLine("```")

                            appendLine()
                        }

                    Desktop
                        .getDesktop()
                        .browse(
                            URI.create(
                                buildString {
                                    append(
                                        "https://github.com/134130/intellij-mise/issues/new",
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

    companion object {
        private val EXCEPTION_EXCLUSIONS =
            setOf(
                Regex("java\\.desktop/java\\.awt\\..+\\.dispatchEvent(Impl)?"),
                Regex("java\\.desktop/java\\.awt\\..+\\.(process|retarget)MouseEvent"),
                Regex("java\\.desktop/javax\\.swing\\..+\\.(process|retarget)MouseEvent"),
                Regex("kotlinx\\.coroutines\\.scheduling\\.CoroutineScheduler"),
            )
    }
}
