package com.github.l34130.mise.rider.run

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.projectView.solutionDirectoryPath
import com.jetbrains.rider.run.PatchCommandLineExtension
import com.jetbrains.rider.run.WorkerRunInfo
import com.jetbrains.rider.runtime.DotNetRuntime
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class RiderPatchCommandLineExtension : PatchCommandLineExtension {
    override fun patchDebugCommandLine(
        lifetime: Lifetime,
        workerRunInfo: WorkerRunInfo,
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
        if (!MiseSettings.getService(project).state.useMiseDirEnv) {
            return
        }

        val envs =
            MiseCommandLine(
                project = project,
                workDir = project.solutionDirectoryPath.toAbsolutePath().toString(),
            ).loadEnvironmentVariables(profile = MiseSettings.getService(project).state.miseProfile)
        commandLine.withEnvironment(envs)
    }
}
