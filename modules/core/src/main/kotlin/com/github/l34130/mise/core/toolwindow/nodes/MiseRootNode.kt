package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.notification.MiseNotificationServiceUtils
import com.github.l34130.mise.core.toolwindow.MiseToolWindowContext
import com.github.l34130.mise.core.toolwindow.MiseToolWindowContextResolver
import com.github.l34130.mise.core.toolwindow.MiseToolWindowState
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths

class MiseRootNode(
    nodeProject: Project,
) : AbstractTreeNode<Any>(nodeProject, "Mise") {
    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Mise"
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val state = project.service<MiseToolWindowState>().state
        val context = project.service<MiseToolWindowContextResolver>().resolve()

        return listOf(
            runCatching { getToolNodes(state.groupByConfigPath, context) }.fold(
                onSuccess = { tools -> MiseToolServiceNode(project, tools) },
                onFailure = { e ->
                    logger.warn("Failed to get tool nodes", e)
                    MiseNotificationServiceUtils.notifyException("Failed to get tool nodes", e, project)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getTaskNodes(context) }.fold(
                onSuccess = { tasks -> MiseTaskServiceNode(project, tasks) },
                onFailure = { e ->
                    logger.warn("Failed to get task nodes", e)
                    MiseNotificationServiceUtils.notifyException("Failed to get task nodes", e, project)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getEnvironmentNodes(context) }.fold(
                onSuccess = { envs -> MiseEnvironmentServiceNode(project, envs) },
                onFailure = { e ->
                    logger.warn("Failed to get settings nodes", e)
                    MiseNotificationServiceUtils.notifyException("Failed to get environment nodes", e, project)
                    MiseErrorNode(project, e)
                },
            ),
        )
    }

    private fun getToolNodes(
        groupByConfigPath: Boolean,
        context: MiseToolWindowContext,
    ): Collection<AbstractTreeNode<*>> {
        val toolsByToolNames =
            MiseCommandLineHelper
                .getDevTools(
                    project = project,
                    workDir = context.workDir,
                    configEnvironment = context.configEnvironment,
                ).getOrThrow()

        if (!groupByConfigPath) {
            return toolsByToolNames
                .flatMap { (toolName, toolInfos) -> toolInfos.map { toolName to it } }
                .sortedBy { (toolName, _) -> toolName.value }
                .map { (toolName, toolInfo) ->
                    MiseToolNode(
                        project = project,
                        toolName = toolName,
                        toolInfo = toolInfo,
                    )
                }
        }

        val toolsBySourcePaths = mutableMapOf<String, MutableList<Pair<MiseDevToolName, MiseDevTool>>>()
        for ((toolName, toolInfos) in toolsByToolNames.entries) {
            for (toolInfo in toolInfos) {
                val sourcePath = toolInfo.source?.absolutePath ?: continue
                val tools = toolsBySourcePaths.getOrPut(sourcePath) { mutableListOf() }
                tools.add(toolName to toolInfo)
            }
        }
        return toolsBySourcePaths
            .map { (sourcePath, tools) ->
                MiseToolConfigDirectoryNode(
                    project = project,
                    configDirPath = sourcePath,
                    tools = tools,
                )
            }
    }

    private fun getEnvironmentNodes(context: MiseToolWindowContext): Collection<MiseEnvironmentNode> {
        val envs =
            MiseCommandLineHelper
                .getEnvVarsExtended(
                    project = project,
                    workDir = context.workDir,
                    configEnvironment = context.configEnvironment,
                ).getOrThrow()

        return envs.map { (key, value) ->
            MiseEnvironmentNode(
                project = project,
                key = key,
                value =
                    if (value.redacted) {
                        "[redacted]"
                    } else {
                        value.value
                    },
            )
        }
    }

    private fun getTaskNodes(
        context: MiseToolWindowContext,
    ): Collection<AbstractTreeNode<*>> {
        val taskResolver = project.service<MiseTaskResolver>()
        val projectBaseDir = context.workDir
        val configEnvironment = context.configEnvironment

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

        val projectTasks: List<MiseTask> = runBlocking { taskResolver.getMiseTasks(false, configEnvironment) }.sortedBy { it.name }
        val trackedConfigs =
            MiseCommandLineHelper
                .getTrackedConfigs(project, configEnvironment, projectBaseDir)
                .onFailure { MiseNotificationServiceUtils.notifyException("Failed to get tracked configs", it, project) }
                .getOrElse { emptyList() }
        val allowedConfigDirs =
            trackedConfigs
                .filter { it.endsWith(".toml") }
                .map { PathUtil.getParentPath(it) }
                .filter { isPathInProjectResolutionChain(projectBaseDir, it) }
                .toSet()
        val filteredTasks =
            projectTasks.filter { task ->
                val taskSourcePath = task.source
                val inProjectTree = isPathInDirectory(taskSourcePath, projectBaseDir)
                val inParentChain = isPathInProjectResolutionChain(projectBaseDir, taskSourcePath)
                val inAllowedConfigDirs = allowedConfigDirs.any { allowedDir -> isPathInDirectory(taskSourcePath, allowedDir) }
                inProjectTree || inParentChain || inAllowedConfigDirs
            }
        for (task in filteredTasks) {
            val taskNode =
                MiseTaskNode(
                    project = project,
                    parent = projectDirNode,
                    taskInfo = task,
                )
            projectDirNode.tasks += taskNode
        }

        nodes += projectDirNode

        return nodes
    }

    private fun isPathInDirectory(
        path: String,
        directory: String,
    ): Boolean {
        val normalizedPath = normalize(path)
        val normalizedDirectory = normalize(directory)
        return normalizedPath.startsWith(normalizedDirectory)
    }

    private fun isPathInProjectResolutionChain(
        projectBaseDir: String,
        candidatePath: String,
    ): Boolean {
        val projectPath = normalize(projectBaseDir)
        val candidate = normalize(candidatePath)
        return projectPath.startsWith(candidate)
    }

    private fun normalize(path: String): Path = Paths.get(path).normalize()

    companion object {
        private val logger =
            Logger.getInstance(MiseRootNode::class.java)
    }
}
