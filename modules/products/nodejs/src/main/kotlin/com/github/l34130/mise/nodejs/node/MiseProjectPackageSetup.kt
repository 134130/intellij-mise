package com.github.l34130.mise.nodejs.node

import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.command.MiseDevToolsScope
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.util.guessMiseProjectPath
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.io.path.Path
import kotlin.reflect.KClass

class MiseProjectPackageSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project): MiseDevToolName? = inspectPackageManager(project)?.let(::MiseDevToolName)

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val currentPackageManager: NodePackage? =
            runReadActionBlocking {
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
                sdkVersion = newPackage.version?.parsedVersion ?: tool.displayVersion,
                sdkPath = newPackage.presentablePath,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Configurable> getConfigurableClass(): KClass<out T> = NodeSettingsConfigurable::class as KClass<out T>

    private fun inspectPackageManager(project: Project): String? {
        val basePath = project.guessMiseProjectPath()
        // Use local requests so unrelated global tools cannot change the project's package manager.
        val tools = MiseCommandLineHelper.getDevTools(
            project = project,
            workDir = basePath,
            configEnvironment = project.service<MiseProjectSettings>().state.miseConfigEnvironment,
            scope = MiseDevToolsScope.LOCAL,
        ).getOrDefault(emptyMap())
        val candidates = tools.filterValues { it.isNotEmpty() }.keys.map { it.value }.toSet()
        return runReadActionBlocking {
            selectPackageManager(
                candidates,
                NpmManager.getInstance(project).`package`?.name,
                LocalFileSystem.getInstance().findFileByNioFile(Path(basePath, NpmUtil.PNPM_LOCK_FILENAME)) != null,
            )
        }
    }

    private fun MiseDevTool.asPackage(project: Project): NodePackage {
        val devToolName = checkNotNull(getDevToolName(project)) { "Cannot select a package manager unambiguously" }.value

        val binPath = getToolBinPath(project, devToolName)
            .getOrElse { e -> throw IllegalStateException("Failed to create NodePackage for $devToolName at path: ${this.resolvedInstallPath}", e) }

        val nodePackage = NpmUtil.DESCRIPTOR.createPackage(binPath)
        check(nodePackage.isValid(project, null)) {
            "Failed to create NodePackage for $devToolName at path: ${this.resolvedInstallPath} (resolved to $binPath)"
        }
        return nodePackage
    }
}

internal fun selectPackageManager(localTools: Set<String>, current: String?, hasPnpmLock: Boolean): String? {
    val candidates = localTools.intersect(setOf("npm", "pnpm", "yarn"))
    return when {
        candidates.size == 1 -> candidates.single()
        candidates.isEmpty() -> if (hasPnpmLock) "pnpm" else "npm"
        current in candidates -> current
        hasPnpmLock && "pnpm" in candidates -> "pnpm"
        else -> null
    }
}
