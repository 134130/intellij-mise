package com.github.l34130.mise.core.setup

import com.github.l34130.mise.core.command.MiseDevToolName

data class MiseDevToolRequest(
    val toolName: MiseDevToolName,
    val canonicalName: String,
)
