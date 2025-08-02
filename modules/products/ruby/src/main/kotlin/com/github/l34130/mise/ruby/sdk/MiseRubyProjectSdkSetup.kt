package com.github.l34130.mise.ruby.sdk

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.plugins.ruby.ruby.sdk.RubySdkType
import kotlin.reflect.KClass

class MiseRubyProjectSdkSetup : AbstractProjectSdkSetup() {
    override fun getDevToolName(): MiseDevToolName = MiseDevToolName("ruby")

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): Boolean {
        val rubySdks = RubySdkType.getAllValidRubySdks()
        val miseSdkName = "mise: ${tool.version}"

        var miseSdk = rubySdks.firstOrNull { it.name == miseSdkName }
        if (miseSdk == null) {
            // Create a new Ruby SDK
            miseSdk = ProjectJdkImpl(miseSdkName, RubySdkType.getInstance())
            miseSdk.homePath = tool.installPath
            miseSdk.versionString = tool.version

            // Add the new SDK to the project JDK table
            ProjectJdkTable.getInstance().addJdk(miseSdk)
        }

        // Set the SDK as the project SDK
        val oldSdk = ProjectRootManager.getInstance(project).projectSdk
        if (oldSdk?.name == miseSdkName) {
            // If the SDK is already set, no need to change
            return false
        }

        // Set the new SDK as the project SDK
        ProjectRootManager.getInstance(project).projectSdk = miseSdk
        return true
    }

    // Application configurable
    // RubyDefaultProjectSdkGemsConfigurable::class as KClass<out T>
    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null
}
