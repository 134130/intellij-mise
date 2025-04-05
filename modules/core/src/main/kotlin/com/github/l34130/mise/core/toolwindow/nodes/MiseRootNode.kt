package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.MiseProjectService
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.model.MiseUnknownTask
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.collapsePath
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class MiseRootNode(
    nodeProject: Project,
) : AbstractTreeNode<Any>(nodeProject, Object()) {
    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Mise"
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val settings = project.service<MiseProjectSettings>()
        val service = project.service<MiseProjectService>()

        runBlocking(Dispatchers.IO) {
            while (!service.isInitialized.get()) {
                delay(100)
            }
        }

        return listOf(
            runCatching { getToolNodes(settings) }.fold(
                onSuccess = { tools -> MiseToolServiceNode(project, tools) },
                onFailure = { e -> MiseErrorNode(project, e) },
            ),
            runCatching { getTaskNodes(settings).sortedBy { it.taskInfo.name } }.fold(
                onSuccess = { tasks -> MiseTaskServiceNode(project, tasks) },
                onFailure = { e -> MiseErrorNode(project, e) },
            ),
            runCatching { getEnvironmentNodes(settings) }.fold(
                onSuccess = { envs -> MiseEnvironmentServiceNode(project, envs) },
                onFailure = { e -> MiseErrorNode(project, e) },
            ),
        )
    }

    private fun getToolNodes(settings: MiseProjectSettings): Collection<MiseToolConfigDirectoryNode> {
        val toolsByToolNames =
            MiseCommandLineHelper
                .getDevTools(
                    workDir = project.basePath,
                    configEnvironment = settings.state.miseConfigEnvironment,
                ).getOrThrow()

        val toolsBySourcePaths = mutableMapOf<String, MutableList<Pair<MiseDevToolName, MiseDevTool>>>()
        for ((toolName, toolInfos) in toolsByToolNames.entries) {
            for (toolInfo in toolInfos) {
                val sourcePath = toolInfo.source?.absolutePath ?: continue
                val tools = toolsBySourcePaths.getOrPut(sourcePath) { mutableListOf() }
                tools.add(toolName to toolInfo)
            }
        }

        return toolsBySourcePaths.map { (sourcePath, tools) ->
            MiseToolConfigDirectoryNode(
                project = project,
                configDirPath = sourcePath,
                tools = tools,
            )
        }
    }

    private fun getEnvironmentNodes(settings: MiseProjectSettings): Collection<MiseEnvironmentNode> {
        val envs =
            MiseCommandLineHelper
                .getEnvVars(
                    workDir = project.basePath,
                    configEnvironment = settings.state.miseConfigEnvironment,
                ).getOrThrow()

        return envs.map { (key, value) ->
            MiseEnvironmentNode(
                project = project,
                key = key,
                value = value,
            )
        }
    }

    private fun getTaskNodes(settings: MiseProjectSettings): Collection<MiseTaskNode> {
        val service = project.service<MiseProjectService>()

        val psiTasks = service.getTasks() // the tasks loaded from psi elements
        val cliTasks =
            MiseCommandLineHelper
                .getTasks(
                    workDir = project.basePath,
                    configEnvironment = settings.state.miseConfigEnvironment,
                ).getOrThrow() // the tasks loaded from command line

        val validPsiTasks = psiTasks.filter { psiTask -> cliTasks.any { it.name == psiTask.name } }

        val filteredCliTasks =
            cliTasks
                .filterNot { task -> psiTasks.any { it.name == task.name } } // the tasks that are not loaded from psi elements

        return validPsiTasks.map {
            MiseTaskNode(
                project = project,
                taskInfo = it,
            )
        } +
            filteredCliTasks.map {
                MiseTaskNode(
                    project = project,
                    taskInfo =
                        MiseUnknownTask(
                            name = it.name,
                            aliases = it.aliases,
                            depends = it.depends,
                            description = it.description,
                            source = it.source?.let { source -> collapsePath(source, project) },
                        ),
                )
            }
    }
}
