package com.github.l34130.mise.core.command

enum class MiseDevToolsScope(
    val cacheKeySegment: String,
    val commandFlag: String?
) {
    LOCAL("local", "--local"),
    GLOBAL("global", "--global"),
    COMBINED("combined", null);

    fun requireCommandFlag(): String {
        return requireNotNull(commandFlag) { "No command flag available for $this scope" }
    }
}
