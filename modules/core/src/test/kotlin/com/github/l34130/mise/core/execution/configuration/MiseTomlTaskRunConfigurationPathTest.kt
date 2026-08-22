package com.github.l34130.mise.core.execution.configuration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MiseTomlTaskRunConfigurationPathTest {
    @Test
    fun `test Windows task directory is converted to system independent relative path`() {
        val cdArgument =
            resolveMiseCdArgument(
                projectBasePath = "C:\\Users\\dev\\project",
                expandedWorkingDirectory = "C:\\Users\\dev\\project\\.mise-tasks\\build",
            )

        assertEquals(".mise-tasks/build", cdArgument)
    }

    @Test
    fun `test Windows parent relative task directory does not contain malformed drive separator`() {
        val cdArgument =
            resolveMiseCdArgument(
                projectBasePath = "C:\\Users\\dev\\project\\module",
                expandedWorkingDirectory = "C:\\Users\\dev\\project\\.mise-tasks\\build",
            )

        assertEquals("../.mise-tasks/build", cdArgument)
        assertFalse(cdArgument.contains(":\\"))
        assertFalse(cdArgument.contains(":/"))
    }

    @Test
    fun `test Windows different drive task directory falls back to absolute path`() {
        val cdArgument =
            resolveMiseCdArgument(
                projectBasePath = "C:\\Users\\dev\\project",
                expandedWorkingDirectory = "D:\\tasks\\build",
            )

        assertEquals("D:/tasks/build", cdArgument)
    }

    @Test
    fun `test Unix task directory keeps existing relative path behavior`() {
        val cdArgument =
            resolveMiseCdArgument(
                projectBasePath = "/home/dev/project",
                expandedWorkingDirectory = "/home/dev/project/.mise-tasks/build",
            )

        assertEquals(".mise-tasks/build", cdArgument)
    }
}
