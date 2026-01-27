package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.ShimUtils
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectPackageSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project): MiseDevToolName = MiseDevToolName(inspectPackageManager(project))

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val currentPackageManager: NodePackage? =
            ReadAction.compute<NodePackage, Throwable> {
                NpmManager.getInstance(project).`package`
            }
        val newPackageManager = tool.asPackage(project)

        if (currentPackageManager == null) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = null,
                requestedInstallPath = newPackageManager.presentablePath,
            )
        }

        if (currentPackageManager.name != newPackageManager.name ||
            currentPackageManager.version?.parsedVersion != newPackageManager.version?.parsedVersion ||
            currentPackageManager.presentablePath != newPackageManager.presentablePath
        ) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentPackageManager.version?.parsedVersion,
                requestedInstallPath = newPackageManager.presentablePath,
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult {
        val packageManager = NpmManager.getInstance(project)
        val newPackage = tool.asPackage(project)

        return WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            packageManager.packageRef = NodePackageRef.create(newPackage)
            ApplySdkResult(
                sdkName = newPackage.name,
                sdkVersion = newPackage.version?.parsedVersion ?: tool.shimsVersion(),
                sdkPath = newPackage.presentablePath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = NodeSettingsConfigurable::class as KClass<out T>

    private fun inspectPackageManager(project: Project): String {
        val basePath =
            requireNotNull(project.guessMiseProjectPath()) {
                "Project $project's base path is null"
            }

        val fileSystem = LocalFileSystem.getInstance()

        val pnpmLockFile = Path(basePath, NpmUtil.PNPM_LOCK_FILENAME)
        val yarnLockFile = Path(basePath, NpmUtil.YARN_LOCK_FILENAME)

        return when {
            fileSystem.findFileByNioFile(pnpmLockFile)?.exists() == true -> "pnpm"
            fileSystem.findFileByNioFile(yarnLockFile)?.exists() == true -> "yarn"
            else -> "npm"
        }
    }

    private fun MiseDevTool.asPackage(project: Project): NodePackage {
        val devToolName = getDevToolName(project).value
        val basePath = WslPathUtils.convertToolPathForWsl(this)
        val path = ShimUtils.findExecutable(basePath, devToolName).path
        val nodePackage = NpmUtil.DESCRIPTOR.createPackage(path)
        check(nodePackage.isValid(project, null)) {
            "Failed to create NodePackage for $devToolName at path: ${this.shimsInstallPath()} (resolved to $path)"
        }
        return nodePackage
    }

    companion object {
        private val logger = logger<MiseProjectPackageSetup>()
    }
}
