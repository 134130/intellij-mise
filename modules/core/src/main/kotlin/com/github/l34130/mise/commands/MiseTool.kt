package com.github.l34130.mise.commands

data class MiseTool(
    val version: String,
    val requestedVersion: String,
    val installPath: String,
    val source: MiseSource,
    val installed: Boolean,
    val active: Boolean,
)
