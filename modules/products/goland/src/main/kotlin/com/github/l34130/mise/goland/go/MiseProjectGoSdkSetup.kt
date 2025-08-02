package com.github.l34130.mise.goland.go

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.goide.configuration.GoSdkConfigurable
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.reflect.KClass

class MiseProjectGoSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName() = MiseDevToolName("go")

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): SetupSdkResult =
        WriteAction.computeAndWait<SetupSdkResult, Throwable> {
            val sdkService = GoSdkService.getInstance(project)
            val beforeSdk = sdkService.getSdk(null)

            val homeDir = LocalFileSystem.getInstance().findFileByPath(tool.installPath)
            val sdkRoot = GoSdkUtil.adjustSdkDir(homeDir)!!

            val newSdk: GoSdk = GoSdkImpl(sdkRoot.url, tool.version, null)
            GoSdkService.getInstance(project).setSdk(newSdk, true)

            if (beforeSdk.homeUrl == newSdk.homeUrl) {
                SetupSdkResult.NoChange(
                    sdkName = newSdk.name,
                    version = newSdk.version,
                    installPath = newSdk.sdkRoot?.path ?: "",
                )
            } else {
                SetupSdkResult.Updated(
                    sdkName = newSdk.name,
                    version = newSdk.version,
                    installPath = newSdk.sdkRoot?.path ?: tool.installPath,
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = GoSdkConfigurable::class as KClass<out T>
}
