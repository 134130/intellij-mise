package com.github.l34130.mise.core.command

import com.google.gson.annotations.SerializedName

data class MiseSource(
    @SerializedName("type")
    val fileName: String, // .mise.toml, .mise.config, etc.
    @SerializedName("path")
    val absolutePath: String, // absolute path to the source file
)
