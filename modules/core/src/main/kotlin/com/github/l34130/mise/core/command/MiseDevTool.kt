package com.github.l34130.mise.core.command

data class MiseDevTool(
    val version: String,
    val requestedVersion: String?,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource?,
) {
    override fun toString(): String =
        "MiseDevTool(version='$version', requestedVersion=$requestedVersion, installPath='$installPath', installed=$installed, active=$active, source=$source)"
}
