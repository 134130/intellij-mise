package com.github.l34130.mise.core.command

data class MiseTask(
    val name: String,
    val aliases: List<String>? = null,
    val depends: List<List<String>>? = null,
    val waitFor: List<List<String>>? = null,
    val dependsPost: List<List<String>>? = null,
    val description: String? = null,
    val hide: Boolean = false,
    val source: String,
    val command: String?,
)
