package com.github.l34130.mise.rider.run

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseCommandLineNotFoundException
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessInfo
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.projectView.solutionDirectoryPath
import com.jetbrains.rider.run.PatchCommandLineExtension
import com.jetbrains.rider.run.WorkerRunInfo
import com.jetbrains.rider.runtime.DotNetRuntime
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class MiseRiderPatchCommandLineExtension : PatchCommandLineExtension {
    override fun patchDebugCommandLine(
        lifetime: Lifetime,
        workerRunInfo: WorkerRunInfo,
        processInfo: ProcessInfo?,
        project: Project,
    ): Promise<WorkerRunInfo> {
        patchCommandLine(workerRunInfo.commandLine, project)
        return resolvedPromise(workerRunInfo)
    }

    override fun patchRunCommandLine(
        commandLine: GeneralCommandLine,
        dotNetRuntime: DotNetRuntime,
        project: Project,
    ): ProcessListener? {
        patchCommandLine(commandLine, project)
        return null
    }

    private fun patchCommandLine(
        commandLine: GeneralCommandLine,
        project: Project,
    ) {
        val projectState = application.service<MiseSettings>().state
        if (!projectState.useMiseDirEnv) {
            return
        }

        val miseEnvVars =
            MiseCommandLineHelper
                .getEnvVars(
                    workDir = project.solutionDirectoryPath.toAbsolutePath().toString(),
                    configEnvironment = projectState.miseConfigEnvironment,
                ).fold(
                    onSuccess = { envVars -> envVars },
                    onFailure = {
                        if (it !is MiseCommandLineNotFoundException) {
                            MiseNotificationServiceUtils.notifyException("Failed to load environment variables", it)
                        }
                        emptyMap()
                    },
                )

        commandLine.withEnvironment(miseEnvVars)
    }
}
