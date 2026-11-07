package com.github.l34130.mise.idea.jdk

import com.github.l34130.mise.core.command.MiseDevTool
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.JavadocOrderRootType
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MiseProjectJdkSetupTest : BasePlatformTestCase() {
    private val tool = MiseDevTool(
        version = "mise-sdk-regression",
        installPath = System.getProperty("java.home"),
        installed = true,
        active = true,
    )

    override fun tearDown() {
        try {
            WriteAction.run<Throwable> {
                ProjectRootManager.getInstance(project).projectSdk = null
                ProjectJdkTable.getInstance().findJdk("${tool.version} (mise)")?.let {
                    ProjectJdkTable.getInstance().removeJdk(it)
                }
            }
        } finally {
            super.tearDown()
        }
    }

    fun `test SDK default updates the effective language level`() {
        val extension = LanguageLevelProjectExtension.getInstance(project)
        WriteAction.run<Throwable> {
            extension.languageLevel = LanguageLevel.JDK_1_8
            extension.default = false
        }

        applySdk()

        val sdk = ProjectRootManager.getInstance(project).projectSdk!!
        assertEquals(true, extension.default)
        assertEquals(JavaSdk.getInstance().getVersion(sdk)!!.maxLanguageLevel, extension.languageLevel)
    }

    fun `test Javadoc is saved when a named SDK already exists`() {
        applySdk()
        val table = ProjectJdkTable.getInstance()
        val sdk = table.findJdk("${tool.version} (mise)")!!
        val rootType = JavadocOrderRootType.getInstance()
        WriteAction.run<Throwable> {
            sdk.sdkModificator.apply {
                removeRoots(rootType)
                commitChanges()
            }
            ProjectRootManager.getInstance(project).projectSdk = null
        }

        applySdk()

        val registeredSdk = table.findJdk(sdk.name)!!
        assertContainsElements(
            registeredSdk.rootProvider.getUrls(rootType).toList(),
            JavaSdk.getInstance().getDefaultDocumentationUrl(registeredSdk)!!,
        )
    }

    private fun applySdk() {
        val method = MiseProjectJdkSetup::class.java.getDeclaredMethod(
            "applySdkConfiguration", MiseDevTool::class.java, Project::class.java,
        )
        method.isAccessible = true
        method.invoke(MiseProjectJdkSetup(), tool, project)
    }
}
