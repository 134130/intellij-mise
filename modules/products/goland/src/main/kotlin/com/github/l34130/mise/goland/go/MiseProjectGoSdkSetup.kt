package com.github.l34130.mise.goland.go

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.goide.configuration.GoSdkConfigurable
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.PathUtil
import org.apache.commons.io.file.PathUtils
import java.io.File
import kotlin.reflect.KClass

class MiseProjectGoSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("go")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val sdkService = GoSdkService.getInstance(project)

        val currentSdk: GoSdk =
            ReadAction.compute<GoSdk, Throwable> {
                sdkService.getSdk(null)
            }
        val newSdk = tool.asGoSdk()

        if (currentSdk == GoSdk.NULL) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = null,
                requestedInstallPath = VfsUtil.urlToPath(newSdk.homeUrl),
            )
        }

        if (currentSdk.name != newSdk.name || currentSdk.homeUrl != newSdk.homeUrl) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = newSdk.version ?: newSdk.majorVersion.name,
                requestedInstallPath = VfsUtil.urlToPath(newSdk.homeUrl),
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val sdkService = GoSdkService.getInstance(project)

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            val sdk = tool.asGoSdk()
            if (sdk == GoSdk.NULL) {
                throw IllegalStateException("Failed to create Go SDK from path: ${tool.installPath}")
            }
            sdkService.setSdk(sdk, true)
            ApplySdkResult(
                sdkName = sdk.name,
                sdkVersion = sdk.version ?: tool.version,
                sdkPath = sdk.homeUrl,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = GoSdkConfigurable::class as KClass<out T>

    private fun MiseDevTool.asGoSdk(): GoSdk {
        var sdk = GoSdk.fromHomePath(this.installPath)
        if (sdk == GoSdk.NULL) {
            // Go 1.25+
            sdk = GoSdk.fromHomePath(this.installPath + File.separator + "go")
        }
        return sdk
    }
}
