package com.github.l34130.mise.core.execution.configuration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MiseTomlTaskRunConfigurationPathTest {
    @Test
    fun `test Windows task directory is converted to system independent absolute path`() {
        val miseCd =
            resolveMiseCd(
                projectBasePath = "C:\\Users\\dev\\project",
                expandedWorkingDirectory = "C:\\Users\\dev\\project\\.mise-tasks\\build",
            )

        assertEquals("C:/Users/dev/project/.mise-tasks/build", miseCd)
    }

    @Test
    fun `test Windows parent task directory remains absolute`() {
        val miseCd =
            resolveMiseCd(
                projectBasePath = "C:\\Users\\dev\\project\\module",
                expandedWorkingDirectory = "C:\\Users\\dev\\project\\.mise-tasks\\build",
            )

        assertEquals("C:/Users/dev/project/.mise-tasks/build", miseCd)
        assertFalse(miseCd.contains("\\"))
    }

    @Test
    fun `test Windows different drive task directory falls back to absolute path`() {
        val miseCd =
            resolveMiseCd(
                projectBasePath = "C:\\Users\\dev\\project",
                expandedWorkingDirectory = "D:\\tasks\\build",
            )

        assertEquals("D:/tasks/build", miseCd)
    }

    @Test
    fun `test Unix task directory remains absolute`() {
        val miseCd =
            resolveMiseCd(
                projectBasePath = "/home/dev/project",
                expandedWorkingDirectory = "/home/dev/project/.mise-tasks/build",
            )

        assertEquals("/home/dev/project/.mise-tasks/build", miseCd)
    }
}
