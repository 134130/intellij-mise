package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectNodeSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName() = MiseDevToolName("node")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)

        val currentInterpreter: NodeJsInterpreter? =
            ReadAction.compute<NodeJsInterpreter?, Throwable> {
                nodeJsInterpreterManager.interpreter
            }
        val newInterpreter = tool.asNodeJsLocalInterpreter()

        if (currentInterpreter == null || !currentInterpreter.deepEquals(newInterpreter)) {
            return SdkStatus.NeedsUpdate(
                currentSdkName = currentInterpreter?.referenceName,
                currentInstallPath =
                    when (currentInterpreter) {
                        is NodeJsLocalInterpreter -> currentInterpreter.interpreterSystemDependentPath
                        else -> "unknown"
                    },
                requestedSdkName = newInterpreter.referenceName,
                requestedInstallPath = newInterpreter.interpreterSystemDependentPath,
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)
        val newInterpreter = tool.asNodeJsLocalInterpreter()

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            nodeJsInterpreterManager.setInterpreterRef(newInterpreter.toRef())
            setupNodePackageManager(project)
            ApplySdkResult(
                sdkName = newInterpreter.referenceName,
                sdkVersion = newInterpreter.cachedVersion?.get()?.parsedVersion ?: tool.version,
                sdkPath = newInterpreter.interpreterSystemDependentPath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

    private fun setupNodePackageManager(project: Project) {
        val packageManager = inspectPackageManager(project)
        try {
            val nodePackage = NodePackageRef.create(packageManager)
            NpmManager.getInstance(project).packageRef = nodePackage
        } catch (e: Exception) {
            throw RuntimeException("Failed to set package manager to $packageManager", e)
        }
    }

    private fun inspectPackageManager(project: Project): String {
        val basePath = project.basePath
        requireNotNull(basePath) {
            "Project $project's base path is null"
        }

        val fileSystem = LocalFileSystem.getInstance()

        val pnpmLockFile = Path(basePath, "pnpm-lock.yaml")
        val yarnLockFile = Path(basePath, "yarn.lock")

        return when {
            fileSystem.findFileByNioFile(pnpmLockFile)?.exists() == true -> "pnpm"
            fileSystem.findFileByNioFile(yarnLockFile)?.exists() == true -> "yarn"
            else -> "npm"
        }
    }

    private fun MiseDevTool.asNodeJsLocalInterpreter(): NodeJsLocalInterpreter {
        val interpreterPath =
            if (SystemInfo.isWindows) {
                Path(FileUtil.expandUserHome(this.installPath), "node.exe")
            } else {
                Path(FileUtil.expandUserHome(this.installPath), "bin", "node")
            }.toAbsolutePath()
                .normalize()
                .toString()

        return NodeJsLocalInterpreter(interpreterPath)
    }
}
