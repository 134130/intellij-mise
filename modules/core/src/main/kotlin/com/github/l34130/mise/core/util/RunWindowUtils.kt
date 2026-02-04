package com.github.l34130.mise.core.util

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.command.MiseVersion
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.intellij.util.execution.ParametersListUtil

object RunWindowUtils {
    fun executeMiseCommand(
        project: Project,
        args: List<String>,
        tabName: String,
        onSuccess: (() -> Unit)? = null,
        onFinish: (() -> Unit)? = null,
    ) {
        val title = "Failed to run $tabName"

        application.invokeLater {
            try {
                val commandLine = buildMiseCommandLine(project, args)
                val processHandler = ColoredProcessHandler(commandLine).apply {
                    setShouldKillProcessSoftly(true)
                    ProcessTerminatedListener.attach(this)
                }

                if (onSuccess != null || onFinish != null) {
                    processHandler.addProcessListener(
                        object : ProcessAdapter() {
                            override fun processTerminated(event: ProcessEvent) {
                                if (event.exitCode == 0) {
                                    onSuccess?.invoke()
                                }
                                // Always notify the caller so they can clear in-flight guards.
                                onFinish?.invoke()
                            }
                        },
                    )
                }

                RunContentExecutor(project, processHandler)
                    .withTitle(tabName)
                    .withRerun { executeMiseCommand(project, args, tabName, onSuccess, onFinish) }
                    .withStop(
                        { processHandler.destroyProcess() },
                        { !processHandler.isProcessTerminated && !processHandler.isProcessTerminating },
                    )
                    .run()
            } catch (e: ExecutionException) {
                onFinish?.invoke()
                MiseNotificationServiceUtils.notifyException(title, e, project)
            }
        }
    }

    private fun buildMiseCommandLine(
        project: Project,
        args: List<String>,
    ): GeneralCommandLine {
        val settings = project.service<MiseProjectSettings>().state
        val executableManager = project.service<MiseExecutableManager>()
        val executablePath = executableManager.getExecutablePath()

        val commandLineArgs = mutableListOf<String>()
        commandLineArgs.addAll(ParametersListUtil.parse(executablePath))

        if (settings.miseConfigEnvironment.isNotBlank()) {
            val miseVersion = executableManager.getExecutableVersion() ?: MiseVersion(0, 0, 0)
            if (miseVersion >= MiseVersion(2024, 12, 2)) {
                commandLineArgs.add("--env")
            } else {
                commandLineArgs.add("--profile")
            }
            commandLineArgs.add(settings.miseConfigEnvironment)
        }

        commandLineArgs.addAll(args)

        val commandLine = GeneralCommandLine(commandLineArgs)
            .withWorkDirectory(project.guessMiseProjectPath())
            // mise writes progress/log output to stderr; merge streams to avoid a red console.
            .withRedirectErrorStream(true)

        MiseCommandLineHelper.environmentSkipCustomization(commandLine.environment)
        return commandLine
    }

}
