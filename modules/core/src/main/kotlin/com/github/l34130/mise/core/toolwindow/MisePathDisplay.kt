package com.github.l34130.mise.core.toolwindow

import com.github.l34130.mise.core.util.presentablePath
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Path
import java.nio.file.Paths

fun displayPath(
    project: Project,
    path: String,
    nonProjectPathDisplay: NonProjectPathDisplay,
): String {
    val projectBasePath = project.basePath ?: return path
    val normalizedPath = runCatching { normalize(path) }.getOrElse { return path }
    val normalizedProjectPath = runCatching { normalize(projectBasePath) }.getOrElse { return path }

    if (normalizedPath.startsWith(normalizedProjectPath)) {
        return FileUtil.getLocationRelativeToUserHome(presentablePath(project, normalizedPath.toString()))
    }

    val rendered =
        when (nonProjectPathDisplay) {
        NonProjectPathDisplay.RELATIVE -> {
            runCatching { normalizedProjectPath.relativize(normalizedPath).toString() }
                .getOrElse { normalizedPath.toString() }
        }

        NonProjectPathDisplay.ABSOLUTE -> normalizedPath.toString()
    }
    return FileUtil.getLocationRelativeToUserHome(rendered)
}

private fun normalize(path: String): Path = Paths.get(path).normalize()
