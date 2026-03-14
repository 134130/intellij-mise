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
import com.github.l34130.mise.core.toolwindow.NonProjectPathDisplay
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
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
        val groupByConfigPath = state.groupByConfigPath
        val nonProjectPathDisplay = state.nonProjectPathDisplay

        return listOf(
            runCatching { getToolNodes(groupByConfigPath, nonProjectPathDisplay, context) }.fold(
                onSuccess = { tools -> MiseToolServiceNode(project, tools) },
                onFailure = { e ->
                    logger.warn("Failed to get tool nodes", e)
                    MiseNotificationServiceUtils.notifyException("Failed to get tool nodes", e, project)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getTaskNodes(groupByConfigPath, nonProjectPathDisplay, context) }.fold(
                onSuccess = { tasks -> MiseTaskServiceNode(project, tasks) },
                onFailure = { e ->
                    logger.warn("Failed to get task nodes", e)
                    MiseNotificationServiceUtils.notifyException("Failed to get task nodes", e, project)
                    MiseErrorNode(project, e)
                },
            ),
            runCatching { getEnvironmentNodes(groupByConfigPath, nonProjectPathDisplay, context) }.fold(
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
        nonProjectPathDisplay: NonProjectPathDisplay,
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
                        nonProjectPathDisplay = nonProjectPathDisplay,
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
            .toList()
            .sortedWith(compareBy({ sortGroupPriority(it.first) }, { sortGroupDepth(it.first) }, { sortGroupName(it.first) }))
            .map { (sourcePath, tools) ->
                MiseToolConfigDirectoryNode(
                    project = project,
                    configDirPath = sourcePath,
                    nonProjectPathDisplay = nonProjectPathDisplay,
                    tools = tools,
                )
            }
    }

    private fun getEnvironmentNodes(
        groupByConfigPath: Boolean,
        nonProjectPathDisplay: NonProjectPathDisplay,
        context: MiseToolWindowContext,
    ): Collection<AbstractTreeNode<*>> {
        val envs =
            MiseCommandLineHelper
                .getEnvVarsExtended(
                    project = project,
                    workDir = context.workDir,
                    configEnvironment = context.configEnvironment,
                ).getOrThrow()

        if (!groupByConfigPath) {
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
                    nonProjectPathDisplay = nonProjectPathDisplay,
                    source = value.source,
                    tool = value.tool,
                )
            }
        }

        val envNodes =
            envs.map { (key, value) ->
                MiseEnvironmentNode(
                    project = project,
                    key = key,
                    value =
                        if (value.redacted) {
                            "[redacted]"
                        } else {
                            value.value
                        },
                    nonProjectPathDisplay = nonProjectPathDisplay,
                    source = value.source,
                    tool = value.tool,
                )
            }

        return envNodes
            .groupBy { normalizeEnvSourceLabel(envs[it.key]?.source) }
            .toList()
            .sortedWith(compareBy({ sortGroupPriority(it.first) }, { sortGroupDepth(it.first) }, { sortGroupName(it.first) }))
            .map { (source, nodes) ->
                MiseEnvironmentConfigDirectoryNode(
                    project = project,
                    configDirPath = source,
                    nonProjectPathDisplay = nonProjectPathDisplay,
                    environments = nodes,
                )
            }
    }

    private fun getTaskNodes(
        groupByConfigPath: Boolean,
        nonProjectPathDisplay: NonProjectPathDisplay,
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
                nonProjectPathDisplay = nonProjectPathDisplay,
                parent = null,
                children = mutableListOf(),
                tasks = mutableListOf(),
            )

        val projectTasks: List<MiseTask> = taskResolver.getCachedTasksOrEmptyList(configEnvironment).sortedBy { it.name }
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

        if (groupByConfigPath) {
            return filteredTasks
                .groupBy { PathUtil.getParentPath(it.source) }
                .toList()
                .sortedWith(compareBy({ sortGroupPriority(it.first) }, { sortGroupDepth(it.first) }, { sortGroupName(it.first) }))
                .map { (sourceDir, tasks) ->
                    val sourceNode =
                        MiseTaskDirectoryNode(
                            project = project,
                            directoryPath = sourceDir,
                            directoryName = StringUtil.ELLIPSIS,
                            nonProjectPathDisplay = nonProjectPathDisplay,
                            parent = null,
                            children = mutableListOf(),
                            tasks = mutableListOf(),
                        )
                    tasks.sortedBy { it.name }.forEach { task ->
                        sourceNode.tasks +=
                            MiseTaskNode(
                                project = project,
                                parent = sourceNode,
                                nonProjectPathDisplay = nonProjectPathDisplay,
                                taskInfo = task,
                            )
                    }
                    sourceNode
                }
        }

        for (task in filteredTasks) {
            val taskNode =
                MiseTaskNode(
                    project = project,
                    parent = projectDirNode,
                    nonProjectPathDisplay = nonProjectPathDisplay,
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

    private fun normalizeEnvSourceLabel(source: String?): String {
        if (source.isNullOrBlank()) return MISE_SYSTEM_ENV_SOURCE_LABEL
        return source
    }

    private fun sortGroupPriority(source: String): Int =
        if (source == MISE_SYSTEM_ENV_SOURCE_LABEL) {
            0
        } else {
            1
        }

    private fun sortGroupDepth(source: String): Int =
        if (isPathLike(source)) {
            runCatching { normalize(source).nameCount }.getOrDefault(Int.MAX_VALUE)
        } else {
            Int.MAX_VALUE
        }

    private fun sortGroupName(source: String): String = source.lowercase()

    private fun isPathLike(source: String): Boolean = source.contains("/") || source.contains("\\")

    companion object {
        private const val MISE_SYSTEM_ENV_SOURCE_LABEL = "Mise System"
        private val logger =
            Logger.getInstance(MiseRootNode::class.java)
    }
}
