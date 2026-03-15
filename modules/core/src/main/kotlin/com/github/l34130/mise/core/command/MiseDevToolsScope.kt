package com.github.l34130.mise.core.command

sealed class MiseDevToolsScope(
    val cacheKeySegment: String,
    val commandFlag: String,
) {
    object LOCAL : MiseDevToolsScope("local", "--local")
    object GLOBAL : MiseDevToolsScope("global", "--global")
}
