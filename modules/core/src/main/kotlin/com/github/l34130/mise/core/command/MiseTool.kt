package com.github.l34130.mise.core.command

data class MiseTool(
    val version: String,
    val requestedVersion: String?,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource?,
)
