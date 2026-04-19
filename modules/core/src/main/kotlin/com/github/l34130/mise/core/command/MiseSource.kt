package com.github.l34130.mise.core.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiseSource(
    @SerialName("type")
    val fileName: String, // .mise.toml, .mise.config, etc.
    @SerialName("path")
    val absolutePath: String, // absolute path to the source file
)
