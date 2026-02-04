package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseCommandLineHelper
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
        val newInterpreter = tool.asNodeJsLocalInterpreter(project)

        if (currentInterpreter == null || !currentInterpreter.deepEquals(newInterpreter)) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentInterpreter?.cachedVersion?.get()?.parsedVersion,
                currentSdkLocation = SdkLocation.Setting,
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ) {
        val nodeJsInterpreterManager = NodeJsInterpreterManager.getInstance(project)
        val newInterpreter = tool.asNodeJsLocalInterpreter(project)

        WriteAction.computeAndWait<Unit, Throwable> {
            nodeJsInterpreterManager.setInterpreterRef(newInterpreter.toRef())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

    private fun MiseDevTool.asNodeJsLocalInterpreter(project: Project): NodeJsLocalInterpreter {
        val nodePath = MiseCommandLineHelper.getBinPath("node", project)
            .getOrElse { throw IllegalStateException("Failed to find NodeJS $displayVersionWithResolved executable: ${it.message}", it) }
        return NodeJsLocalInterpreter(nodePath)
    }
}
