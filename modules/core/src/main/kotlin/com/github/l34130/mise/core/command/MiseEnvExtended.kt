package com.github.l34130.mise.core.command

data class MiseEnvExtended(
    val value: String,
    val source: String? = null,
    val tool: String? = null,
    val redacted: Boolean = false,
)
