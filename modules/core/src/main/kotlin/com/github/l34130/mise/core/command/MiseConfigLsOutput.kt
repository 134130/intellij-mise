package com.github.l34130.mise.core.command

import kotlinx.serialization.Serializable

@Serializable
data class MiseConfigLsOutput(
    val path: String,
)
