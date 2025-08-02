package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.javascript.nodejs.util.NodePackageRef
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

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): SetupSdkResult =
        WriteAction.computeAndWait<SetupSdkResult, Throwable> {
            val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)
            val oldInterpreter = nodeJsInterpreterManager.interpreter

            val newNodePath =
                if (SystemInfo.isWindows) {
                    Path(FileUtil.expandUserHome(tool.installPath), "node.exe")
                } else {
                    Path(FileUtil.expandUserHome(tool.installPath), "bin", "node")
                }.toAbsolutePath()
                    .normalize()
                    .toString()
            val newInterpreter = NodeJsLocalInterpreter(newNodePath)

            // Setup NodeJS Interpreter
            nodeJsInterpreterManager.setInterpreterRef(newInterpreter.toRef())

            // Setup Node Package Manager
            val packageManager = inspectPackageManager(project)
            try {
                val nodePackage = NodePackageRef.create(packageManager)
                NpmManager.getInstance(project).packageRef = nodePackage
            } catch (e: Exception) {
                throw RuntimeException("Failed to set package manager to $packageManager", e)
            }

            if (oldInterpreter?.referenceName == newInterpreter.referenceName) {
                SetupSdkResult.NoChange(
                    sdkName = oldInterpreter.referenceName,
                    version = oldInterpreter.cachedVersion?.get()?.parsedVersion ?: tool.version,
                    installPath = newInterpreter.interpreterSystemDependentPath,
                )
            } else {
                SetupSdkResult.Updated(
                    sdkName = newInterpreter.referenceName,
                    version = newInterpreter.cachedVersion?.get()?.parsedVersion ?: tool.version,
                    installPath = newInterpreter.interpreterSystemDependentPath,
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

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
}
