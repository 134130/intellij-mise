package com.github.l34130.mise.core.command

import kotlinx.serialization.Serializable

@Serializable
data class MiseEnv(
    val value: String,
    val source: String? = null,
    val tool: String? = null,
)
