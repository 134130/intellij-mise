package com.github.l34130.mise.maven

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.idea.maven.project.MavenInSpecificPath

class MavenTest : BasePlatformTestCase() {
    fun `test MavenInSpecificPath stores and returns home path`() {
        val path = "/fake/maven/3.9.6"
        val homeType = MavenInSpecificPath(path)
        assertEquals(path, homeType.mavenHome)
    }
}
