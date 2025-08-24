package com.github.l34130.mise.core.toolwindow.nodes

import com.github.l34130.mise.core.MiseProjectService
import com.github.l34130.mise.core.MiseTaskResolver
import com.github.l34130.mise.core.command.MiseCommandLineHelper
import com.github.l34130.mise.core.command.MiseDevTool
import com.github.l34130.mise.core.command.MiseDevToolName
import com.github.l34130.mise.core.model.MiseTask
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.github.l34130.mise.core.util.presentablePath
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
import fleet.util.computeIfAbsentShim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.Queue

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

//        val service = project.service<MiseProjectService>()
//
//        val psiTasks = service.getTasks() // the tasks loaded from psi elements
//        val cliTasks =
//            MiseCommandLineHelper
//                .getTasks(
//                    workDir = project.basePath,
//                    configEnvironment = settings.state.miseConfigEnvironment,
//                ).getOrThrow() // the tasks loaded from command line
//
//        val nodes = mutableListOf<AbstractTreeNode<*>>()
//
//        val validPsiTasks: List<MiseTask> = psiTasks.filter { psiTask -> cliTasks.any { it.name == psiTask.name } }
//        val filteredCliTasks: List<MiseUnknownTask> =
//            cliTasks
//                .filterNot { task -> psiTasks.any { it.name == task.name } } // the tasks that are not loaded from psi elements
//                .map {
//                    MiseUnknownTask(
//                        name = it.name,
//                        aliases = it.aliases,
//                        depends = it.depends,
//                        description = it.description,
//                        source = it.source,
//                    )
//                }
//
//        // 1. 모든 디렉터리 노드를 경로(path)를 키로 하여 저장하고 관리하는 캐시
//        val directoryNodeCache = mutableMapOf<String, MiseTaskDirectoryNode>()
//
//        // 기존 코드와 동일하게 작업을 디렉터리별로 그룹화
//        val tasksByDirectory =
//            (validPsiTasks + filteredCliTasks)
//                .groupBy { PathUtil.getParentPath(it.source) }
//
//        // 2. 각 디렉터리 및 그 부모 디렉터리에 대한 노드를 생성하고 부모-자식 관계 설정
//        tasksByDirectory.forEach { (directoryPath, tasks) ->
//            // 현재 디렉터리 노드를 가져오거나 생성 (이 노드는 작업을 포함)
//            val taskDirectoryNode =
//                directoryNodeCache.getOrPut(directoryPath) {
//                    MiseTaskDirectoryNode(
//                        project = project,
//                        directoryPath = directoryPath,
//                        parent = null,
//                        children = mutableListOf(),
//                        tasks = mutableListOf(),
//                    )
//                }
//
//            // 작업 노드(MiseTaskNode)들을 생성하여 디렉터리 노드에 추가
//            tasks.forEach { task ->
//                val taskNode =
//                    MiseTaskNode(
//                        project = project,
//                        parent = taskDirectoryNode,
//                        taskInfo = task,
//                    )
//                taskDirectoryNode.tasks.add(taskNode)
//            }
//            taskDirectoryNode.tasks.sortBy { it.taskInfo.name }
//
//            // 3. 현재 디렉터리의 상위 경로를 따라 올라가며 부모 노드들을 생성하고 관계를 연결
//            var parentPath = PathUtil.getParentPath(directoryPath)
//            var childNode = taskDirectoryNode
//
//            while (parentPath != null && parentPath != childNode.directoryPath) {
//                val parentNode =
//                    directoryNodeCache.getOrPut(parentPath) {
//                        MiseTaskDirectoryNode(
//                            project = project,
//                            directoryPath = parentPath,
//                            parent = null,
//                            children = mutableListOf(),
//                            tasks = mutableListOf(),
//                        )
//                    }
//
//                // 부모-자식 관계 연결 (중복 추가 방지)
//                if (!parentNode.children.contains(childNode)) {
//                    parentNode.children.add(childNode)
//                }
//
//                // 다음 순회를 위해 현재 노드와 경로를 부모로 변경
//                childNode = parentNode
//                parentPath = PathUtil.getParentPath(parentPath)
//            }
//        }
//
//        // 4. 전체 노드 중에서 부모가 없는 최상위 노드들만 필터링하여 최종 결과 생성
//        nodes +=
//            directoryNodeCache.values
//                .filter { node ->
//                    // 부모 경로가 없거나, 그 부모가 캐시에 없는 경우 최상위 노드로 간주
//                    val parentPath = PathUtil.getParentPath(node.directoryPath)
//                    parentPath == null || !directoryNodeCache.containsKey(parentPath) || parentPath == node.directoryPath
//                }.sortedBy { it.directoryPath }
//
//        // (선택) 각 레벨의 자식 노드들도 정렬
//        directoryNodeCache.values.forEach { it.children.sortBy { child -> child.directoryPath } }
//
//        return nodes
    }

    companion object {
        private val logger =
            Logger.getInstance(MiseRootNode::class.java)
    }

    private class DirectoryNode(
        val full: String,
        val part: String,
        val children: MutableMap<String, DirectoryNode> = mutableMapOf(),
    )
}
