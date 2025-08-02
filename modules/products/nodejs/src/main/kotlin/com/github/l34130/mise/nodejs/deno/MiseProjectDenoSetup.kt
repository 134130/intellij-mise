package com.github.l34130.mise.nodejs.deno

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.deno.DenoConfigurable
import com.intellij.deno.DenoSettings
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectDenoSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName() = MiseDevToolName("deno")

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): SetupSdkResult =
        WriteAction.computeAndWait<SetupSdkResult, Throwable> {
            val settings = DenoSettings.getService(project)

            val oldDenoPath = settings.getDenoPath()
            val newDenoPath =
                Path(FileUtil.expandUserHome(tool.installPath), "bin", "deno")
                    .toAbsolutePath()
                    .normalize()
                    .toString()

            if (oldDenoPath == newDenoPath) {
                SetupSdkResult.NoChange(
                    sdkName = "deno",
                    version = tool.version,
                    installPath = oldDenoPath,
                )
            } else {
                settings.setDenoPath(newDenoPath)
                SetupSdkResult.Updated(
                    sdkName = "deno",
                    version = tool.version,
                    installPath = newDenoPath,
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = DenoConfigurable::class as KClass<out T>
}
