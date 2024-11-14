package com.github.l34130.mise.nodejs.deno

import com.github.l34130.mise.core.commands.MiseTool
import com.github.l34130.mise.core.setups.AbstractProjectSdkSetup
import com.github.l34130.mise.core.setups.MiseToolRequest
import com.intellij.deno.DenoConfigurable
import com.intellij.deno.DenoSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import kotlin.io.path.Path
import kotlin.reflect.KClass

class ProjectDenoSetup : AbstractProjectSdkSetup() {
    override fun getToolRequest(): MiseToolRequest =
        MiseToolRequest(
            name = "deno",
            canonicalName = "Deno",
        )

    override fun setupSdk(
        tool: MiseTool,
        project: Project,
    ): Boolean {
        val settings = project.service<DenoSettings>()

        val oldDenoPath = settings.getDenoPath()
        val newDenoPath =
            Path(FileUtil.expandUserHome(tool.installPath), "bin", "deno")
                .toAbsolutePath()
                .normalize()
                .toString()

        if (oldDenoPath == newDenoPath) {
            return false
        }

        settings.setDenoPath(newDenoPath)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = DenoConfigurable::class as KClass<out T>
}
