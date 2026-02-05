package com.github.l34130.mise.core.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MiseCommandLineHelperTest {
    @Test
    fun `merge dev tools uses local overrides and keeps global`() {
        val local = parseDevTools(LOCAL_PROJECT_JSON)
        val global = parseDevTools(GLOBAL_HOME_JSON)

        val merged = MiseCommandLineHelper.mergeDevTools(local, global)

        val elixir = merged[MiseDevToolName("elixir")]?.single()
        assertNotNull(elixir)
        assertEquals("1.17.3-otp-27", elixir?.version)

        val erlang = merged[MiseDevToolName("erlang")]?.single()
        assertNotNull(erlang)
        assertEquals("27.1.1", erlang?.version)

        val ruby = merged[MiseDevToolName("ruby")]?.single()
        assertNotNull(ruby)
        assertEquals("4.0.1", ruby?.version)

        assertEquals(5, merged.size)
    }

    @Test
    fun `merge dev tools returns global when local empty`() {
        val local = parseDevTools("{}")
        val global = parseDevTools(GLOBAL_HOME_JSON)

        val merged = MiseCommandLineHelper.mergeDevTools(local, global)

        assertEquals(global, merged)
    }

    private fun parseDevTools(json: String): Map<MiseDevToolName, List<MiseDevTool>> {
        val raw: Map<String, List<MiseDevTool>> = MiseCommandLineOutputParser.parse(json)
        return raw.mapKeys { (toolName, _) -> MiseDevToolName(toolName) }
    }

    companion object {
        private val LOCAL_PROJECT_JSON =
            """
            {
              "elixir": [
                {
                  "version": "1.17.3-otp-27",
                  "requested_version": "1.17.3-otp-27",
                  "install_path": "/home/user/.local/share/mise/installs/elixir/1.17.3-otp-27",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/workspace/ic_headend_simulators/mise.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ],
              "erlang": [
                {
                  "version": "27.1.1",
                  "requested_version": "27.1.1",
                  "install_path": "/home/user/.local/share/mise/installs/erlang/27.1.1",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/workspace/ic_headend_simulators/mise.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ],
              "java": [
                {
                  "version": "jetbrains-21.0.9-b895.149",
                  "requested_version": "jetbrains-21.0.9",
                  "install_path": "/home/user/.local/share/mise/installs/java/jetbrains-21.0.9-b895.149",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/workspace/ic_headend_simulators/mise.local.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ],
              "postgres": [
                {
                  "version": "16.11",
                  "requested_version": "16.11",
                  "install_path": "/home/user/.local/share/mise/installs/postgres/16.11",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/workspace/ic_headend_simulators/mise.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ]
            }
            """.trimIndent()

        private val GLOBAL_HOME_JSON =
            """
            {
              "elixir": [
                {
                  "version": "1.19.5-otp-28",
                  "requested_version": "latest",
                  "install_path": "/home/user/.local/share/mise/installs/elixir/1.19.5-otp-28",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/.config/mise/config.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ],
              "erlang": [
                {
                  "version": "28.2",
                  "requested_version": "latest",
                  "install_path": "/home/user/.local/share/mise/installs/erlang/28.2",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/.config/mise/config.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ],
              "ruby": [
                {
                  "version": "4.0.1",
                  "requested_version": "latest",
                  "install_path": "/home/user/.local/share/mise/installs/ruby/4.0.1",
                  "source": {
                    "type": "mise.toml",
                    "path": "/home/user/.config/mise/config.toml"
                  },
                  "installed": true,
                  "active": true
                }
              ]
            }
            """.trimIndent()
    }
}
