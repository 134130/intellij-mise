package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectInterpreterSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("node")

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
                currentSdkVersion = currentInterpreter?.cachedVersion?.get()?.parsedVersion,
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
            ApplySdkResult(
                sdkName = newInterpreter.presentableName,
                sdkVersion = newInterpreter.cachedVersion?.get()?.parsedVersion ?: tool.shimsVersion(),
                sdkPath = newInterpreter.interpreterSystemDependentPath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

    private fun MiseDevTool.asNodeJsLocalInterpreter(): NodeJsLocalInterpreter {
        val basePath = WslPathUtils.convertToolPathForWsl(this)

        val interpreterPath =
            if (SystemInfo.isWindows) {
                // For WSL UNC paths, maintain Unix structure (no .exe)
                if (basePath.startsWith("\\\\")) {
                    Path(FileUtil.expandUserHome(basePath), "bin", "node")
                } else {
                    Path(FileUtil.expandUserHome(basePath), "node.exe")
                }
            } else {
                Path(FileUtil.expandUserHome(basePath), "bin", "node")
            }.toAbsolutePath()
                .normalize()
                .toString()

        return NodeJsLocalInterpreter(interpreterPath)
    }

    companion object {
        private val logger = logger<MiseProjectInterpreterSetup>()
    }
}
