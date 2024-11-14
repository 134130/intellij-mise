package com.github.l34130.mise.core.command

data class MiseSource(
    val fileName: String, // .mise.toml, .mise.config, etc.
    val absolutePath: String, // absolute path to the source file
)
