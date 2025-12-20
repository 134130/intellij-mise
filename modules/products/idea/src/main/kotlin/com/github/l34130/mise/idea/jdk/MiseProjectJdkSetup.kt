package com.github.l34130.mise.idea.jdk

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.wsl.WslPathUtils
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import kotlin.reflect.KClass

class MiseProjectJdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(project: Project) = MiseDevToolName("java")

    override fun checkSdkStatus(
        tool: MiseDevTool,
        project: Project,
    ): SdkStatus {
        val currentSdk = ProjectRootManager.getInstance(project).projectSdk
        val newSdk = tool.asJavaSdk()

        if (currentSdk == null || currentSdk.name != newSdk.name || currentSdk.homePath != newSdk.homePath) {
            return SdkStatus.NeedsUpdate(
                currentSdkVersion = currentSdk?.versionString,
                requestedInstallPath = newSdk.homePath ?: tool.shimsInstallPath(),
            )
        }

        return SdkStatus.UpToDate
    }

    override fun applySdkConfiguration(
        tool: MiseDevTool,
        project: Project,
    ): ApplySdkResult =
        WriteAction.computeAndWait<ApplySdkResult, Throwable> {
            val projectJdkTable = ProjectJdkTable.getInstance()

            val sdk =
                tool.asJavaSdk().also { sdk ->
                    val oldJdk = projectJdkTable.findJdk(tool.jdkName())
                    if (oldJdk != null) {
                        projectJdkTable.updateJdk(oldJdk, sdk)
                    } else {
                        projectJdkTable.addJdk(sdk)
                    }
                }

            ProjectRootManager.getInstance(project).projectSdk = sdk
            ApplySdkResult(
                sdkName = sdk.name,
                sdkVersion = sdk.versionString ?: tool.shimsVersion(),
                sdkPath = sdk.homePath ?: tool.shimsInstallPath(),
            )
        }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null

    private fun MiseDevTool.asJavaSdk(): Sdk {
        val sdkPath = WslPathUtils.convertToolPathForWsl(this)
        return JavaSdk.getInstance().createJdk(this.jdkName(), sdkPath, false)
    }

    private fun MiseDevTool.jdkName(): String = "${this.shimsVersion()} (mise)"

    companion object {
        private val logger = logger<MiseProjectJdkSetup>()
    }
}
