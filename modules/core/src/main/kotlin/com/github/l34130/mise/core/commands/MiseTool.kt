package com.github.l34130.mise.core.commands

data class MiseTool(
    val version: String,
    val requestedVersion: String?,
    val installPath: String,
    val installed: Boolean,
    val active: Boolean,
    val source: MiseSource?,
)
