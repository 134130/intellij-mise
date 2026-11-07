package com.github.l34130.mise.idea.jdk

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JavadocOrderRootType
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import kotlin.reflect.KClass

class MiseProjectJdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("java")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val currentSdk = ProjectRootManager.getInstance(project).projectSdk
        val newSdk = tool.asJavaSdk()

        if (currentSdk == null) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = null,
                requestedInstallPath = newSdk.homePath ?: tool.resolvedInstallPath,
            )
        }

        if (isSamePath(currentSdk.homePath, newSdk.homePath)) {
            return SdkStatus.UpToDate
        }

        val displayVersion =
            if (currentSdk.sdkType is JavaSdk) {
                val javaSdk = JavaSdk.getInstance()
                val canonicalName = currentSdk.homePath?.let { javaSdk.suggestSdkName(null, it) } ?: currentSdk.name
                val version = sanitizeVersion(javaSdk.getVersionString(currentSdk))
                if (version != null) {
                    "$canonicalName ($version)"
                } else {
                    canonicalName
                }
            } else {
                currentSdk.name
            }

        return SdkStatus.NeedsUpdate(
            currentSdkVersion = displayVersion,
            requestedInstallPath = newSdk.homePath ?: tool.resolvedInstallPath,
        )
    }

    private fun sanitizeVersion(version: String?): String? {
        if (version == null) return null
        val removables = listOf("Azul Zulu", "Eclipse Temurin", "Oracle OpenJDK", "java version", "[\"]")
        return version
            .replace(Regex(removables.joinToString("|"), RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-?\\s*(aarch64|x86_64|x64|amd64)"), "")
            .trim()
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult =
        WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            val projectJdkTable = ProjectJdkTable.getInstance()

            val sdk =
                tool.asJavaSdk().also { sdk ->
                    configureJavadoc(sdk)
                    val oldJdk = projectJdkTable.findJdk(tool.jdkName())
                    if (oldJdk != null) {
                        projectJdkTable.updateJdk(oldJdk, sdk)
                    } else {
                        projectJdkTable.addJdk(sdk)
                    }
                }

            LanguageLevelProjectExtension.getInstance(project)?.default = true
            ProjectRootManager.getInstance(project).projectSdk = sdk
            ApplySdkResult(
                sdkName = sdk.name,
                sdkVersion = sdk.versionString ?: tool.displayVersion,
                sdkPath = sdk.homePath ?: tool.resolvedInstallPath,
            )
        }

    /**
     * Adds the default Javadoc root to the SDK if available.
     * A write-lock is required so this must be called from [applySdkConfiguration] not [asJavaSdk]
     */
    private fun configureJavadoc(sdk: Sdk) {
        val sdkModificator = sdk.sdkModificator
        val javadocRootType = JavadocOrderRootType.getInstance()
        if (sdkModificator.getRoots(javadocRootType).isEmpty()) {
            JavaSdk.getInstance().getDefaultDocumentationUrl(sdk)?.also {
                sdkModificator.addRoot(it, javadocRootType)
                sdkModificator.commitChanges()
            }
        }
    }

    private fun isSamePath(path1: String?, path2: String?): Boolean {
        if (path1 == null || path2 == null) return false
        return FileUtil.filesEqual(File(path1), File(path2))
    }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null

    private fun MiseDevTool.asJavaSdk(): Sdk {
        return JavaSdk.getInstance().createJdk(this.jdkName(), resolvedInstallPath, false)
    }

    private fun MiseDevTool.jdkName(): String = "${this.displayVersion} (mise)"
}
