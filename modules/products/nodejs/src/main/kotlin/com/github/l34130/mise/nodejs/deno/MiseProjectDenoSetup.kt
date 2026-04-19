package com.github.l34130.mise.nodejs.deno

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.deno.DenoConfigurable
import com.intellij.deno.DenoSettings
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

class MiseProjectDenoSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("deno")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val settings = DenoSettings.getService(project)

        val currentDenoPath: String? =
            ReadAction.compute<String?, Throwable> {
                settings.getDenoPath()
            }
        val newDenoPath = tool.asDenoPath(project)

        return if (currentDenoPath == newDenoPath) {
            SdkStatus.UpToDate
        } else {
            SdkStatus.NeedsUpdate(
                currentSdkVersion = null,
                requestedInstallPath = newDenoPath,
            )
        }
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val settings = DenoSettings.getService(project)
        val newDenoPath = tool.asDenoPath(project)

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            settings.setDenoPath(newDenoPath)
            ApplySdkResult(
                sdkName = "deno",
                sdkVersion = tool.displayVersion,
                sdkPath = newDenoPath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = DenoConfigurable::class as KClass<out T>

    private fun MiseDevTool.asDenoPath(project: Project): String =
        getToolBinPath(project)
            .getOrElse { e -> throw IllegalStateException("Failed to find bin path for ${getDevToolName(project).value}", e) }
}
