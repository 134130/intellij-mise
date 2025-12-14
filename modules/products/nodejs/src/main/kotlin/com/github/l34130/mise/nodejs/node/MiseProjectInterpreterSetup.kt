package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
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
        val interpreterPath =
            if (SystemInfo.isWindows) {
                Path(FileUtil.expandUserHome(this.shimsInstallPath()), "node.exe")
            } else {
                Path(FileUtil.expandUserHome(this.shimsInstallPath()), "bin", "node")
            }.toAbsolutePath()
                .normalize()
                .toString()

        return NodeJsLocalInterpreter(interpreterPath)
    }
}
