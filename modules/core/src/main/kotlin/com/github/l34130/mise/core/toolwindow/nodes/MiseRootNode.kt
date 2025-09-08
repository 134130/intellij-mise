package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.MiseProjectService
import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths

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
                onFailure = { e ->
                    logger.warn("Failed to get tool nodes", e)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getTaskNodes() }.fold(
                onSuccess = { tasks -> MiseTaskServiceNode(project, tasks) },
                onFailure = { e ->
                    logger.warn("Failed to get task nodes", e)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getEnvironmentNodes(settings) }.fold(
                onSuccess = { envs -> MiseEnvironmentServiceNode(project, envs) },
                onFailure = { e ->
                    logger.warn("Failed to get settings nodes", e)
                    MiseErrorNode(project, e)
                },
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

    private fun getTaskNodes(): Collection<AbstractTreeNode<*>> {
        val taskResolver = project.service<MiseTaskResolver>()
        val projectBaseDir = project.basePath ?: ProjectUtil.getBaseDir()

        val nodes = mutableListOf<AbstractTreeNode<*>>()

        // --- Base Project ---
        val projectDirNode =
            MiseTaskDirectoryNode(
                project = project,
                directoryPath = projectBaseDir,
                directoryName = StringUtil.ELLIPSIS,
                parent = null,
                children = mutableListOf(),
                tasks = mutableListOf(),
            )

        val projectTasks: List<MiseTask> = runBlocking { taskResolver.getMiseTasks(projectBaseDir) }.sortedBy { it.name }
        for (task in projectTasks) {
            val taskNode =
                MiseTaskNode(
                    project = project,
                    parent = projectDirNode,
                    taskInfo = task,
                )
            projectDirNode.tasks += taskNode
        }

        nodes += projectDirNode

        // --- Sub Directories ---
        val trackedConfigs = MiseCommandLineHelper.getTrackedConfigs().getOrElse { emptyList() }
        val subDirs: List<String> =
            trackedConfigs
                .filter { it.startsWith(projectBaseDir) }
                .filter { it.endsWith(".toml") }
                .map { PathUtil.getParentPath(it) }
                .distinct()

        for (subDir in subDirs) {
            val relativePath = subDir.removePrefix(projectBaseDir).trim(File.separatorChar)
            if (relativePath.isEmpty()) continue

            val parts = relativePath.split(File.separator)
            var currentParent: MiseTaskDirectoryNode = projectDirNode
            var currentPath = projectBaseDir

            for (part in parts) {
                currentPath = Paths.get(currentPath, part).toString()

                var dirNode = currentParent.children.find { it.directoryName == part }
                if (dirNode == null) {
                    dirNode =
                        MiseTaskDirectoryNode(
                            project = project,
                            directoryPath = currentPath,
                            directoryName = part,
                            parent = currentParent,
                            children = mutableListOf(),
                            tasks = mutableListOf(),
                        )

                    currentParent.children += dirNode

                    val taskInfos: List<MiseTask> = runBlocking { taskResolver.getMiseTasks(dirNode.directoryPath) }.sortedBy { it.name }
                    for (taskInfo in taskInfos) {
                        val taskNode =
                            MiseTaskNode(
                                project = project,
                                parent = dirNode,
                                taskInfo = taskInfo,
                            )
                        dirNode.tasks += taskNode
                    }
                }

                currentParent = dirNode
            }
        }

        return nodes
    }

    companion object {
        private val logger =
            Logger.getInstance(MiseRootNode::class.java)
    }
}
