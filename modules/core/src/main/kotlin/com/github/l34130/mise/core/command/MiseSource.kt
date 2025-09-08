package com.github.l34130.mise.core.command

import com.fasterxml.jackson.annotation.JsonProperty

data class MiseSource(
    @JsonProperty("type")
    val fileName: String, // .mise.toml, .mise.config, etc.
    @JsonProperty("path")
    val absolutePath: String, // absolute path to the source file
)
