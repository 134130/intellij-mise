package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
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
import com.intellij.util.EnvironmentUtil
import com.intellij.util.application
import org.jdom.Element

class MiseTomlTaskRunConfiguration(
    project: Project,
    factory: MiseTomlTaskRunConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<RunProfileState>(project, factory, name) {
    private val settings = application.service<MiseSettings>().state

    var miseExecutablePath: String = settings.executablePath
    var miseConfigEnvironment: String? = settings.miseConfigEnvironment
    var miseTaskName: String = ""
    var workingDirectory: String? = project.basePath
    var envVars: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getState(
        executor: Executor,
        executionEnvironment: ExecutionEnvironment,
    ): RunProfileState {
        return object : CommandLineState(executionEnvironment) {
            override fun startProcess(): ProcessHandler {
                val macroManager = PathMacroManager.getInstance(project)
                val workDirectory =
                    workingDirectory?.let { macroManager.expandPath(it) }
                        ?: project.basePath

                val params = mutableListOf<String>()
                miseConfigEnvironment?.let {
                    params += listOf("--env", it)
                }
                params += listOf("run", miseTaskName)

                val commandLine =
                    GeneralCommandLine()
                        .withExePath(miseExecutablePath)
                        .withWorkDirectory(workDirectory)
                        .withEnvironment(EnvironmentUtil.getEnvironmentMap() + envVars.envs)
                        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                        .withParameters(params)
                        .withCharset(EncodingManager.getInstance().defaultConsoleEncoding)

                return ColoredProcessHandler(commandLine).apply {
                    setShouldKillProcessSoftly(true)
                    ProcessTerminatedListener.attach(this)
                }
            }
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = MiseTomlTaskRunConfigurationEditor(project)

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        val child = element.getOrCreateChild("mise")
        child.setAttribute("executablePath", miseExecutablePath)
        child.setAttribute("configEnvironment", miseConfigEnvironment ?: "")
        child.setAttribute("taskName", miseTaskName)
        child.setAttribute("workingDirectory", workingDirectory ?: "")
        envVars.writeExternal(child)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val child = element.getChild("mise") ?: return
        miseExecutablePath = child.getAttributeValue("executablePath") ?: ""
        miseConfigEnvironment = child.getAttributeValue("configEnvironment")
        miseTaskName = child.getAttributeValue("taskName") ?: ""
        workingDirectory = child.getAttributeValue("workingDirectory")
        envVars = EnvironmentVariablesData.readExternal(child)
    }
}
