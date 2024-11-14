package com.github.l34130.mise.core.commands

data class MiseSource(
    val type: String, // .mise.toml, .mise.config, etc.
    val path: String, // absolute path to the source file
)
