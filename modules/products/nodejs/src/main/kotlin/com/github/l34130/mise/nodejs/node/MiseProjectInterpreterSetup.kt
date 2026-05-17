package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.interpreter.wsl.WslNodeInterpreter
import com.intellij.javascript.nodejs.interpreter.wsl.WslNodeInterpreterManager
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
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
        val newInterpreter = tool.asNodeJsInterpreter(project)

        if (currentInterpreter == null || !currentInterpreter.deepEquals(newInterpreter)) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentInterpreter?.cachedVersion?.get()?.parsedVersion,
                requestedInstallPath = newInterpreter.presentableName,
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)
        val newInterpreter = tool.asNodeJsInterpreter(project)

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            if (newInterpreter is WslNodeInterpreter) {
                registerWslInterpreter(newInterpreter)
            }
            nodeJsInterpreterManager.setInterpreterRef(newInterpreter.toRef())
            ApplySdkResult(
                sdkName = newInterpreter.presentableName,
                sdkVersion = newInterpreter.cachedVersion?.get()?.parsedVersion ?: tool.displayVersion,
                sdkPath = newInterpreter.presentableName,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

    private fun MiseDevTool.asNodeJsInterpreter(project: Project): NodeJsInterpreter {
        val devToolName = getDevToolName(project).value
        val binPath = getToolBinPath(project)
            .getOrElse { e -> throw IllegalStateException("Failed to create Node interpreter for $devToolName", e) }
        return createNodeJsInterpreter(binPath, this.wslDistributionMsId)
    }

    private fun registerWslInterpreter(interpreter: WslNodeInterpreter) {
        val manager = WslNodeInterpreterManager.getInstance() ?: return
        val existing = manager.interpreters
        val alreadyRegistered = existing.any {
            it.wslDistributionId == interpreter.wslDistributionId &&
                it.wslInterpreterPath == interpreter.wslInterpreterPath
        }
        if (!alreadyRegistered) {
            manager.interpreters = existing + interpreter
        }
    }

    companion object {
        /**
         * Picks the right [NodeJsInterpreter] flavor for a mise-managed node binary.
         *
         * For WSL-resolved tools, IntelliJ requires [WslNodeInterpreter] referenced as
         * `wsl://<distro>@/<unix-path>` — feeding it the `\\wsl.localhost\<distro>\...` UNC path
         * via [NodeJsLocalInterpreter] yields a broken `C:/home/<user>/...` resolution.
         * See https://github.com/134130/intellij-mise/issues/476.
         */
        internal fun createNodeJsInterpreter(binPath: String, wslDistributionMsId: String?): NodeJsInterpreter {
            if (wslDistributionMsId != null) {
                val unixBinPath = WslPathUtils.convertWindowsUncToUnixPath(binPath) ?: binPath
                return WslNodeInterpreter(wslDistributionMsId, unixBinPath)
            }
            return NodeJsLocalInterpreter(binPath)
        }
    }
}
