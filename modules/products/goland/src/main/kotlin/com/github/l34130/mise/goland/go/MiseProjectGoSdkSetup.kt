package com.github.l34130.mise.goland.go

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.goide.configuration.GoSdkConfigurable
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.nio.file.Paths
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

        // Compare paths using canonical/real paths to handle symlinks properly
        if (!isSamePath(currentSdk.homeUrl, newSdk.homeUrl)) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentSdk.version ?: currentSdk.majorVersion.name,
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
                throw IllegalStateException("Failed to create Go SDK from path: ${tool.shimsInstallPath()}")
            }
            sdkService.setSdk(sdk, true)
            ApplySdkResult(
                sdkName = sdk.name,
                sdkVersion = sdk.version ?: tool.shimsVersion(),
                sdkPath = sdk.homeUrl,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = GoSdkConfigurable::class as KClass<out T>

    private fun MiseDevTool.asGoSdk(): GoSdk {
        val basePath = WslPathUtils.convertToolPathForWsl(this)
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
        try {
            val path1 = VfsUtil.urlToPath(url1)
            val path2 = VfsUtil.urlToPath(url2)
            
            // Direct comparison first
            if (path1 == path2) {
                return true
            }
            
            val file1 = Paths.get(path1)
            val file2 = Paths.get(path2)
            
            // Use Files.isSameFile which is specifically designed for this purpose
            // It handles symlinks and different path representations efficiently
            if (java.nio.file.Files.exists(file1) && java.nio.file.Files.exists(file2)) {
                return java.nio.file.Files.isSameFile(file1, file2)
            }
            
            // If files don't exist, compare normalized absolute paths
            return File(path1).absoluteFile.normalize() == File(path2).absoluteFile.normalize()
        } catch (e: Exception) {
            logger.warn("Failed to compare paths: $url1 vs $url2", e)
            // Fall back to simple string comparison if path resolution fails
            return url1 == url2
        }
    }

    companion object {
        private val logger = logger<MiseProjectGoSdkSetup>()
    }
}
