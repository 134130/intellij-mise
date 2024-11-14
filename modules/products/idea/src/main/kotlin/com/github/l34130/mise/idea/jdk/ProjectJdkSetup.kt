package com.github.l34130.mise.idea.jdk

import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.setup.AbstractProjectSdkSetup
import com.github.l34130.mise.core.setup.MiseToolRequest
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import kotlin.reflect.KClass

class ProjectJdkSetup : AbstractProjectSdkSetup() {
    override fun getToolRequest(): MiseToolRequest =
        MiseToolRequest(
            name = "java",
            canonicalName = "JDK",
        )

    override fun setupSdk(
        tool: MiseDevTool,
        project: Project,
    ): Boolean {
        val projectJdkTable = ProjectJdkTable.getInstance()

        cleanDeprecatedMiseJdks(projectJdkTable) // TODO: Drop after later version

        val jdkName = "${tool.requestedVersion ?: tool.version} (mise)"

        val oldJdk = projectJdkTable.findJdk(jdkName)
        val newJdk =
            JavaSdk.getInstance().createJdk(
                jdkName,
                tool.installPath,
                false, // isJre
            )

        if (oldJdk != null) {
            projectJdkTable.updateJdk(oldJdk, newJdk)
        } else {
            projectJdkTable.addJdk(newJdk)
        }

        ProjectRootManager.getInstance(project).projectSdk = newJdk

        return oldJdk?.homePath != newJdk.homePath
    }

    override fun <T : Configurable> getConfigurableClass(): KClass<out T>? = null

    // Clean deprecated JDKs (before Mise 2.3.0)
    private fun cleanDeprecatedMiseJdks(projectJdkTable: ProjectJdkTable) {
        val regex = Regex("JDK of project .+ (.*mise.+)")

        val allJdks = projectJdkTable.allJdks

        for (jdk in allJdks) {
            runCatching {
                val matches = regex.matches(jdk.name)
                if (matches) {
                    projectJdkTable.removeJdk(jdk)
                }
            }
        }
    }
}
