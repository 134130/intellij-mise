package com.github.l34130.mise.goland.go

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.goide.configuration.GoSdkConfigurable
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
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
                currentSdkLocation = SdkLocation.Project,
            )
        }

        // Compare versions
        if (currentSdk.version == newSdk.version) {
            return SdkStatus.UpToDate
        }

        // Compare paths using canonical/real paths to handle symlinks properly
        if (isSamePath(currentSdk.homeUrl, newSdk.homeUrl)) {
            return SdkStatus.UpToDate
        }

        return SdkStatus.NeedsUpdate(
            currentSdkVersion = currentSdk.version ?: currentSdk.majorVersion.name,
            currentSdkLocation = SdkLocation.Project,
        )
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ) {
        val sdkService = GoSdkService.getInstance(project)

        WriteAction.computeAndWait<Unit, Throwable> {
            val sdk = tool.asGoSdk()
            if (sdk == GoSdk.NULL) {
                throw IllegalStateException("Failed to create Go SDK from path: ${tool.resolvedInstallPath}")
            }
            sdkService.setSdk(sdk, true)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getSettingsConfigurableClass(): KClass<out T> =
        GoSdkConfigurable::class as KClass<out T>

    private fun MiseDevTool.asGoSdk(): GoSdk {
        val basePath = resolvedInstallPath
        var sdk = GoSdk.fromHomePath(basePath)
        if (sdk == GoSdk.NULL) {
            // Go 1.25+ - try with /go subdirectory
            sdk = GoSdk.fromHomePath(basePath + File.separator + "go")
        }
        return sdk
    }

    /**
     * Compares two paths (URLs) to determine if they point to the same location.
     * This handles symlinks and different path representations.
     */
    private fun isSamePath(url1: String, url2: String): Boolean {
        val path1 = VfsUtil.urlToPath(url1)
        val path2 = VfsUtil.urlToPath(url2)
        return FileUtil.filesEqual(File(path1), File(path2))
    }
}
