package com.github.l34130.mise.goland.go

import com.github.l34130.mise.core.commands.MiseTool
import com.github.l34130.mise.core.setups.AbstractProjectSdkSetup
import com.github.l34130.mise.core.setups.MiseToolRequest
import com.goide.configuration.GoSdkConfigurable
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.reflect.KClass

class ProjectGoSdkSetup : AbstractProjectSdkSetup() {
    override fun getToolRequest(): MiseToolRequest =
        MiseToolRequest(
            name = "go",
            canonicalName = "Go",
        )

    override fun setupSdk(
        tool: MiseTool,
        project: Project,
    ): Boolean {
        val sdkService = GoSdkService.getInstance(project)
        val beforeSdk = sdkService.getSdk(null)

        val homeDir = LocalFileSystem.getInstance().findFileByPath(tool.installPath)
        val sdkRoot = GoSdkUtil.adjustSdkDir(homeDir)!!

        val newSdk: GoSdk = GoSdkImpl(sdkRoot.url, tool.version, null)
        GoSdkService.getInstance(project).setSdk(newSdk, true)

        return beforeSdk.homeUrl != newSdk.homeUrl
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = GoSdkConfigurable::class as KClass<out T>
}
