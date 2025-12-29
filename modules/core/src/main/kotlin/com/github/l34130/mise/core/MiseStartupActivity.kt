package com.github.l34130.mise.core

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class MiseStartupActivity :
    ProjectActivity,
    DumbAware {
    override suspend fun execute(project: Project) {

    }
}
