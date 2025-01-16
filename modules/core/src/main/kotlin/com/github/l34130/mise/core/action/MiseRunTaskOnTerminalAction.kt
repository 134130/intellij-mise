package com.github.l34130.mise.core.action

import com.github.l34130.mise.core.util.TerminalUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

class MiseRunTaskOnTerminalAction(
    private val taskName: String,
    private val configEnvironment: String? = null,
) : DumbAwareAction(
        "Run Mise Task",
        "Execute the Mise task on Terminal",
        AllIcons.Actions.Execute,
    ) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            LOG.warn("Project is null on ${MiseRunTaskOnTerminalAction::class.simpleName}.actionPerformed(AnActionEvent)")
            return
        }

        executeTask(project, taskName, configEnvironment)
    }

    companion object {
        private val LOG = Logger.getInstance(MiseRunTaskOnTerminalAction::class.java)

        fun executeTask(
            project: Project,
            taskName: String,
            configEnvironment: String? = null,
        ) {
            val command =
                buildString {
                    append("mise run")
                    configEnvironment?.let { append(" --profile '$it'") } // TODO: Handle mise config environment
                    append(" '$taskName'")
                }

            TerminalUtils.executeCommand(project, command, taskName)
        }
    }
}
