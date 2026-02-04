package com.github.l34130.mise.nodejs.deno

import com.github.l34130.mise.core.command.MiseCommandLineHelper
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
                currentSdkLocation = SdkLocation.Setting,
            )
        }
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ) {
        val settings = DenoSettings.getService(project)
        val newDenoPath = tool.asDenoPath(project)

        WriteAction.computeAndWait<Unit, Throwable> {
            settings.setDenoPath(newDenoPath)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T> =
        DenoConfigurable::class as KClass<out T>

    private fun MiseDevTool.asDenoPath(project: Project): String {
        return MiseCommandLineHelper.getBinPath("deno", project)
            .getOrElse { throw IllegalStateException("Failed to find Deno $displayVersionWithResolved executable: ${it.message}", it) }
    }
}
