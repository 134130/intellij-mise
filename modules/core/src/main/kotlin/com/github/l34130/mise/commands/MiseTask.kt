package com.github.l34130.mise.commands

data class MiseTask(
    val name: String,
    val aliases: List<String>? = null,
    val depends: List<String>? = null,
    val description: String? = null,
    val hide: Boolean = false,
    val source: String? = null,
) {
    override fun toString(): String =
        "MiseTask(name='$name', aliases=$aliases, depends=$depends, description=$description, hide=$hide, source=$source)"
}
