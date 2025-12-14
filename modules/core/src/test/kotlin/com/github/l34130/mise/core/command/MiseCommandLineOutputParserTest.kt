package com.github.l34130.mise.core.command

import org.junit.Test

class MiseCommandLineOutputParserTest {
    @Test
    fun `parse MiseTask`() {
        val output =
            """
            {
              "name": "hello-world",
              "aliases": [],
              "description": "",
              "source": "/Users/cooper/development/mise-test/mise.toml",
              "depends": [
                "echo"
              ],
              "depends_post": [],
              "wait_for": [],
              "env": {},
              "dir": null,
              "hide": false,
              "raw": false,
              "sources": [],
              "outputs": [],
              "shell": null,
              "quiet": false,
              "silent": false,
              "tools": {},
              "run": [],
              "file": null
            }
            """.trimIndent()

        val actual: MiseTask = MiseCommandLineOutputParser.parse(output)
        val expected =
            MiseTask(
                name = "hello-world",
                aliases = emptyList(),
                depends = listOf(listOf("echo")),
                waitFor = emptyList(),
                dependsPost = emptyList(),
                description = "",
                hide = false,
                source = "/Users/cooper/development/mise-test/mise.toml",
                command = null,
            )

        assert(actual == expected)
    }

    @Test
    fun `parse MiseTask with shell script depends`() {
        val output =
            """
            {
              "name": "hello-world",
              "aliases": [],
              "description": "",
              "source": "/Users/cooper/development/mise-test/mise.toml",
              "depends": [
                [
                  "echo",
                  "'Hello",
                  "World'"
                ]
              ],
              "depends_post": [],
              "wait_for": [],
              "env": {},
              "dir": null,
              "hide": false,
              "raw": false,
              "sources": [],
              "outputs": [],
              "shell": null,
              "quiet": false,
              "silent": false,
              "tools": {},
              "run": [],
              "file": null
            }
            """.trimIndent()

        val actual: MiseTask = MiseCommandLineOutputParser.parse(output)
        val expected =
            MiseTask(
                name = "hello-world",
                aliases = emptyList(),
                depends = listOf(listOf("echo", "'Hello", "World'")),
                waitFor = emptyList(),
                dependsPost = emptyList(),
                description = "",
                hide = false,
                source = "/Users/cooper/development/mise-test/mise.toml",
                command = null,
            )
        assert(actual == expected)
    }

    @Test
    fun `parse MiseDevTool with all fields`() {
        val output =
            """
            {
              "version": "22.17.1",
              "requested_version": "22.17.1",
              "install_path": "/Users/user/.local/share/mise/installs/node/22.17.1",
              "installed": true,
              "active": true,
              "source": {
                "type": "mise.toml",
                "path": "/Users/user/project/mise.toml"
              }
            }
            """.trimIndent()

        val actual: MiseDevTool = MiseCommandLineOutputParser.parse(output)
        val expected =
            MiseDevTool(
                version = "22.17.1",
                requestedVersion = "22.17.1",
                installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
                installed = true,
                active = true,
                source = MiseSource(
                    fileName = "mise.toml",
                    absolutePath = "/Users/user/project/mise.toml"
                ),
            )

        assert(actual == expected)
    }

    @Test
    fun `parse MiseDevTool without optional fields`() {
        val output =
            """
            {
              "version": "22.17.1",
              "install_path": "/Users/user/.local/share/mise/installs/node/22.17.1",
              "installed": true,
              "active": true
            }
            """.trimIndent()

        val actual: MiseDevTool = MiseCommandLineOutputParser.parse(output)
        val expected =
            MiseDevTool(
                version = "22.17.1",
                requestedVersion = null,
                installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
                installed = true,
                active = true,
                source = null,
            )

        assert(actual == expected)
    }

    @Test
    fun `parse Map of MiseDevTools`() {
        val output =
            """
            {
              "node": [
                {
                  "version": "22.17.1",
                  "requested_version": "22.17.1",
                  "install_path": "/Users/user/.local/share/mise/installs/node/22.17.1",
                  "installed": true,
                  "active": true,
                  "source": {
                    "type": "mise.toml",
                    "path": "/Users/user/project/mise.toml"
                  }
                }
              ]
            }
            """.trimIndent()

        val actual: Map<String, List<MiseDevTool>> = MiseCommandLineOutputParser.parse(output)
        val expected = mapOf(
            "node" to listOf(
                MiseDevTool(
                    version = "22.17.1",
                    requestedVersion = "22.17.1",
                    installPath = "/Users/user/.local/share/mise/installs/node/22.17.1",
                    installed = true,
                    active = true,
                    source = MiseSource(
                        fileName = "mise.toml",
                        absolutePath = "/Users/user/project/mise.toml"
                    ),
                )
            )
        )

        assert(actual == expected)
    }
}
