package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.util.application

class MiseTomlTaskRunConfiguration(
    project: Project,
    factory: MiseTomlTaskRunConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<RunProfileState>(project, factory, name) {
    override fun getState(
        executor: Executor,
        executionEnvironment: ExecutionEnvironment,
    ): RunProfileState {
        val settings = application.service<MiseSettings>().state
        val executablePath = settings.executablePath

        return object : CommandLineState(executionEnvironment) {
            override fun startProcess(): ProcessHandler {
                val macroManager = PathMacroManager.getInstance(project)
                // val localFile = macroManager.expandPath()

                val commandLine =
                    GeneralCommandLine()
                        .withExePath(executablePath)
//                    .withWorkDirectory()
//                    .withEnvironment()
                        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                        .withParameters(listOf())
                        .withCharset(EncodingManager.getInstance().defaultConsoleEncoding)

                val processHandler = ColoredProcessHandler(commandLine)
                processHandler.setShouldKillProcessSoftly(true)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }
}
