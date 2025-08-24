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
import com.intellij.openapi.util.io.FileUtil
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectDenoSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName() = MiseDevToolName("deno")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val settings = DenoSettings.getService(project)

        val currentDenoPath: String? =
            ReadAction.compute<String?, Throwable> {
                settings.getDenoPath()
            }
        val newDenoPath = tool.asDenoPath()

        return if (currentDenoPath == newDenoPath) {
            SdkStatus.UpToDate
        } else {
            SdkStatus.NeedsUpdate(
                currentInstallPath = currentDenoPath,
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
        val newDenoPath = tool.asDenoPath()

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            settings.setDenoPath(newDenoPath)
            ApplySdkResult(
                sdkName = "deno",
                sdkVersion = tool.version,
                sdkPath = newDenoPath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = DenoConfigurable::class as KClass<out T>

    private fun MiseDevTool.asDenoPath() =
        Path(FileUtil.expandUserHome(this.installPath), "bin", "deno")
            .toAbsolutePath()
            .normalize()
            .toString()
}
