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
}
