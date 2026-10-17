package com.github.l34130.mise.nodejs.run

import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.lang.javascript.buildTools.npm.rc.NpmConfigurationType
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class MiseNodeRunConfigurationExtensionTest : BasePlatformTestCase() {
    fun `test npm scripts use their package directory and preserve explicit manager`() {
        val configuration = NpmConfigurationType().createTemplateConfiguration(project) as NpmRunConfiguration
        val packageJson = myFixture.addFileToProject("packages/app/package.json", "{}").virtualFile
        val explicitManager = NodePackageRef.create("/custom/pnpm")
        configuration.runSettings = NpmRunSettings.builder()
            .setPackageJsonPath(packageJson.path)
            .setPackageManagerPackageRef(explicitManager)
            .build()

        assertEquals(
            File(packageJson.path).parent,
            MiseNodeRunConfigurationExtension().getWorkingDirectory(configuration),
        )
        assertEquals(explicitManager, configuration.runSettings.packageManagerPackageRef)
    }

    fun `test project package manager reference is preserved`() {
        val configuration = NpmConfigurationType().createTemplateConfiguration(project) as NpmRunConfiguration
        configuration.runSettings = NpmRunSettings.builder()
            .setPackageJsonPath("")
            .setPackageManagerPackageRef(NpmUtil.createProjectPackageManagerPackageRef())
            .build()

        assertNull(MiseNodeRunConfigurationExtension().getWorkingDirectory(configuration))
        assertTrue(NpmUtil.isProjectPackageManagerPackageRef(configuration.runSettings.packageManagerPackageRef))
    }
}
