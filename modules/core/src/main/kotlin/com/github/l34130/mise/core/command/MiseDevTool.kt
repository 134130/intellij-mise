package com.github.l34130.mise.core.command

data class MiseDevTool(
    val version: String,
    val requestedVersion: String? = null,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource? = null,
) {
    fun shimsVersion(): String = requestedVersion ?: version

    fun shimsInstallPath(): String = installPath
}
