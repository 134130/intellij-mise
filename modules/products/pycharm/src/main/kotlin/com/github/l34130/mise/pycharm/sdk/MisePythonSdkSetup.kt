package com.github.l34130.mise.pycharm.sdk

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
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
    override fun getDevToolName(): MiseDevToolName = MiseDevToolName("python")

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): SetupSdkResult {
        val configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment
        val useUv =
            // Check if the 'settings.python.uv_venv_auto' is set to true
            MiseCommandLineHelper
                .getConfig(project.basePath, configEnvironment, "settings.python.uv_venv_auto")
                .getOrNull()
                ?.trim()
                ?.toBoolean() ?: false

        if (!useUv) {
            throw UnsupportedOperationException("Mise Python SDK setup requires 'settings.python.uv_venv_auto' to be true.")
        }

        val pythonSdks = PythonSdkUtil.getAllSdks()
        val uvSdkName = "uv (python)"

        var uvSdk: Sdk? = pythonSdks.firstOrNull { it.name == uvSdkName }
        if (uvSdk == null) {
            uvSdk =
                ProjectJdkImpl(
                    uvSdkName,
                    PythonSdkType.getInstance(),
                    // Find the Python executable using the 'which python' command
                    MiseCommandLineHelper
                        .executeCommand(project.basePath, configEnvironment, listOf("which", "python"))
                        .getOrElse { throw IllegalStateException("Failed to find Python executable: ${it.message}") },
                    // Get the Python version using the 'python --version' command
                    MiseCommandLineHelper
                        .executeCommand(project.basePath, configEnvironment, listOf("python", "--version"))
                        .getOrElse { throw IllegalStateException("Failed to get Python version: ${it.message}") }
                        .replace("Python ", "")
                        .trim(),
                )
        }

        // Set the SDK as the project SDK
        val oldSdk = ProjectRootManager.getInstance(project).projectSdk
        if (oldSdk?.name == uvSdkName) {
            // If the SDK is already set, no need to change
            return SetupSdkResult.NoChange(
                sdkName = uvSdk.name,
                version = uvSdk.versionString,
                installPath = uvSdk.homePath ?: tool.installPath,
            )
        }

        // Set the new SDK as the project SDK
        WriteAction.runAndWait<Throwable> {
            ProjectRootManager.getInstance(project).projectSdk = uvSdk
        }
        return SetupSdkResult.Updated(
            sdkName = uvSdk.name,
            version = uvSdk.versionString,
            installPath = uvSdk.homePath ?: tool.installPath,
        )
    }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null
}
