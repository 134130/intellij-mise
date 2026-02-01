package com.github.l34130.mise.core.execution.configuration

import com.github.l34130.mise.core.command.MiseExecutableManager
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.util.EnvironmentUtil
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import java.io.File

class MiseTomlTaskRunConfiguration(
    project: Project,
    factory: MiseTomlTaskRunConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<RunProfileState>(project, factory, name) {
    private val projectSettings = project.service<MiseProjectSettings>().state

    var miseConfigEnvironment: String = projectSettings.miseConfigEnvironment
    var miseTaskName: String = ""
    var workingDirectory: String = project.guessMiseProjectPath()
    var envVars: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
    var taskParams: String = ""

    override fun getState(
        executor: Executor,
        executionEnvironment: ExecutionEnvironment,
    ): RunProfileState {
        return object : CommandLineState(executionEnvironment) {
            override fun startProcess(): ProcessHandler {
                val projectBasePath = project.guessMiseProjectPath()

                val macroManager = PathMacroManager.getInstance(project)
                val expandedWorkingDirectory = macroManager.expandPath(workingDirectory)
                val workDirectory =
                    FileUtil.getRelativePath(projectBasePath, expandedWorkingDirectory, File.separatorChar) ?: expandedWorkingDirectory

                val params = mutableListOf<String>()
                params += "-C"
                params += workDirectory
                if (miseConfigEnvironment.isNotBlank()) {
                    params += listOf("--env", miseConfigEnvironment)
                }
                params += listOf("run", miseTaskName)
                if (taskParams.isNotBlank()) {
                    params += "--"
                    params += ParametersListUtil.parse(taskParams)
                }

                val commandLine = PtyCommandLine()
                if (!SystemInfo.isWindows) {
                    commandLine.withEnvironment("TERM", "xterm-256color")
                }
                commandLine.withConsoleMode(false)
                commandLine.withInitialColumns(120)
                commandLine.withCharset(EncodingManager.getInstance().defaultConsoleEncoding)
                commandLine.withEnvironment(EnvironmentUtil.getEnvironmentMap() + envVars.envs)
                commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                commandLine.withWorkDirectory(projectBasePath)
                commandLine.withParameters(params)

                val executableManager = project.service<MiseExecutableManager>()
                val miseExecutablePath = executableManager.getExecutablePath()
                commandLine.withExePath(miseExecutablePath)

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
        child.setAttribute("configEnvironment", miseConfigEnvironment)
        child.setAttribute("taskName", miseTaskName)
        child.setAttribute("workingDirectory", workingDirectory)
        child.setAttribute("taskParams", taskParams)
        envVars.writeExternal(child)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val child = element.getChild("mise") ?: return
        miseConfigEnvironment = child.getAttributeValue("configEnvironment")
        miseTaskName = child.getAttributeValue("taskName") ?: ""
        workingDirectory = child.getAttributeValue("workingDirectory")
        taskParams = child.getAttributeValue("taskParams") ?: ""
        envVars = EnvironmentVariablesData.readExternal(child)
    }
}
