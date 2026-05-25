package com.github.l34130.mise.core.command

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiseCommandLineHelperProjectConfigsTest {

    @Test
    fun `mergeProjectConfigs filters out unrelated project configs`() {
        val activeConfigs = listOf(
            "/home/user/workspace/my-project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )
        val trackedConfigs = listOf(
            "/home/user/workspace/my-project/mise.toml",
            "/home/user/workspace/my-project/server/mise.toml",
            "/home/user/workspace/other-project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/my-project",
        )

        assertEquals(
            listOf(
                "/home/user/.config/mise/config.toml",
                "/home/user/workspace/my-project/mise.toml",
                "/home/user/workspace/my-project/server/mise.toml",
            ),
            result,
        )
    }

    @Test
    fun `mergeProjectConfigs orders from general to specific`() {
        val activeConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )
        val trackedConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/project",
        )

        // Global config should come first, project root second
        assertEquals("/home/user/.config/mise/config.toml", result[0])
        assertEquals("/home/user/workspace/project/mise.toml", result[1])
    }

    @Test
    fun `mergeProjectConfigs deduplicates paths`() {
        val activeConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )
        val trackedConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
            "/home/user/workspace/project/sub/mise.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/project",
        )

        // mise.toml appears in both activeConfigs and trackedConfigs but should appear only once
        assertEquals(3, result.size)
        assertEquals(
            listOf(
                "/home/user/.config/mise/config.toml",
                "/home/user/workspace/project/mise.toml",
                "/home/user/workspace/project/sub/mise.toml",
            ),
            result,
        )
    }

    @Test
    fun `mergeProjectConfigs handles empty activeConfigs`() {
        val trackedConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
            "/home/user/workspace/other/mise.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = emptyList(),
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/project",
        )

        assertEquals(listOf("/home/user/workspace/project/mise.toml"), result)
    }

    @Test
    fun `mergeProjectConfigs handles empty trackedConfigs`() {
        val activeConfigs = listOf(
            "/home/user/workspace/project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = emptyList(),
            workDir = "/home/user/workspace/project",
        )

        assertEquals(
            listOf(
                "/home/user/.config/mise/config.toml",
                "/home/user/workspace/project/mise.toml",
            ),
            result,
        )
    }

    @Test
    fun `mergeProjectConfigs handles both empty`() {
        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = emptyList(),
            trackedConfigs = emptyList(),
            workDir = "/home/user/workspace/project",
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `mergeProjectConfigs filters multiple unrelated projects`() {
        val activeConfigs = listOf(
            "/home/user/workspace/intellij-mise/mise.toml",
            "/home/user/.config/mise/config.toml",
        )
        val trackedConfigs = listOf(
            "/home/user/workspace/intellij-mise/mise.toml",
            "/home/user/workspace/intellij-mise/modules/core/mise.toml",
            "/home/user/workspace/gang-e-nae/mise.toml",
            "/home/user/workspace/gang-e-nae/backend/mise.toml",
            "/home/user/workspace/another-project/mise.toml",
            "/home/user/.config/mise/config.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/intellij-mise",
        )

        // Only global config + intellij-mise configs should be present
        assertEquals(
            listOf(
                "/home/user/.config/mise/config.toml",
                "/home/user/workspace/intellij-mise/mise.toml",
                "/home/user/workspace/intellij-mise/modules/core/mise.toml",
            ),
            result,
        )
    }

    @Test
    fun `mergeProjectConfigs does not include project with similar prefix`() {
        val activeConfigs = listOf(
            "/home/user/workspace/app/mise.toml",
            "/home/user/.config/mise/config.toml",
        )
        val trackedConfigs = listOf(
            "/home/user/workspace/app/mise.toml",
            "/home/user/workspace/app-other/mise.toml",
            "/home/user/workspace/application/mise.toml",
        )

        val result = MiseCommandLineHelper.mergeProjectConfigs(
            activeConfigs = activeConfigs,
            trackedConfigs = trackedConfigs,
            workDir = "/home/user/workspace/app",
        )

        // "app-other" and "application" should NOT be included even though they start with "app"
        // because path normalization uses full path segments
        assertEquals(
            listOf(
                "/home/user/.config/mise/config.toml",
                "/home/user/workspace/app/mise.toml",
            ),
            result,
        )
    }
}
