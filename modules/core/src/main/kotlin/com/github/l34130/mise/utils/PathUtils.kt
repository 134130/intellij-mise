package com.github.l34130.mise.utils

object PathUtils {
    fun getHomeDir(): String = System.getProperty("user.home")

    fun abbrHomeDir(path: String): String =
        when {
            path.startsWith("~") -> path
            path.startsWith(getHomeDir()) -> "~" + path.substring(getHomeDir().length)
            else -> path
        }
}
