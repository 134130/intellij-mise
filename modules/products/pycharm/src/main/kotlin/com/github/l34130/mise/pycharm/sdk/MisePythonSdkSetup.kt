package com.github.l34130.mise.pycharm.sdk

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import kotlin.reflect.KClass

class MisePythonSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project): MiseDevToolName = MiseDevToolName("python")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        checkUvEnabled(project)

        val currentSdk: Sdk? =
            ReadAction.compute<Sdk?, Throwable> {
                ProjectRootManager.getInstance(project).projectSdk
            }
        val newSdk = tool.asUvSdk(project)

        if (currentSdk == null || !currentSdk.homePath.equals(newSdk.homePath)) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentSdk?.versionString,
                requestedInstallPath = newSdk.homePath ?: tool.shimsInstallPath(),
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val newSdk = tool.asUvSdk(project)

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            ProjectRootManager.getInstance(project).projectSdk = newSdk
            ApplySdkResult(
                sdkName = newSdk.name,
                sdkVersion = newSdk.versionString ?: tool.shimsVersion(),
                sdkPath = newSdk.homePath ?: tool.shimsInstallPath(),
            )
        }
    }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null

    private fun checkUvEnabled(project: Project) {
        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
        val useUv =
            // Check if the 'settings.python.uv_venv_auto' is set to true
            MiseCommandLineHelper
                .getConfig(project, project.guessMiseProjectPath(), configEnvironment, "settings.python.uv_venv_auto")
                .getOrNull()
                ?.trim()
                ?.toBoolean() ?: false

        if (!useUv) {
            throw UnsupportedOperationException("Mise Python SDK setup requires 'settings.python.uv_venv_auto' to be true.")
        }
    }

    private fun MiseDevTool.asUvSdk(project: Project): Sdk {
        val exists = PythonSdkUtil.getAllSdks().firstOrNull { it.name == uvSdkName() }
        if (exists != null) {
            return exists
        }

        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment

        // Get Python path from 'which python' command (returns Unix path in WSL)
        val pythonUnixPath =
            MiseCommandLineHelper
                .executeCommand(project, project.guessMiseProjectPath(), configEnvironment, listOf("which", "python"))
                .getOrElse { throw IllegalStateException("Failed to find Python executable: ${it.message}") }
                .trim()

        // Convert to Windows UNC path if in WSL mode using the shared utility
        val pythonPath = WslPathUtils.convertUnixPathForWsl(pythonUnixPath)

        // Get Python version
        val pythonVersion =
            MiseCommandLineHelper
                .executeCommand(project, project.guessMiseProjectPath(), configEnvironment, listOf("python", "--version"))
                .getOrElse { throw IllegalStateException("Failed to get Python version: ${it.message}") }
                .replace("Python ", "")
                .trim()

        return ProjectJdkImpl(
            uvSdkName(),
            PythonSdkType.getInstance(),
            pythonPath,
            pythonVersion,
        )
    }

    private fun MiseDevTool.uvSdkName() = "uv (python)"
}
