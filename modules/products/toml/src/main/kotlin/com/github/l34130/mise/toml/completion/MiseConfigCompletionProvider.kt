package com.github.l34130.mise.toml.completion

import com.github.l34130.mise.core.command.MiseCommandLine
import com.github.l34130.mise.core.command.MiseTask
import com.github.l34130.mise.core.icon.MiseIcons
import com.github.l34130.mise.core.setting.MiseSettings
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ProcessingContext
import kotlin.io.path.Path

fun normalizePath(path: String) =
    Path(
        FileUtil.expandUserHome(path),
    ).normalize().toAbsolutePath().toString()

class MiseConfigCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        params: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet,
    ) {
        val position = params.position
        val root = position.containingFile

        if (!MiseConfigPsiUtils.isInTaskDepends(position)) {
            return
        }

        val profile = MiseSettings.getService(root.project).state.miseProfile
        // TODO: Store the tasks in a cache and update them only when the file changes
        //    This will prevent unnecessary calls to the CLI or when the file is an invalid state
        val tasksFromMise =
            MiseCommandLine(
                project = root.project,
                workDir = root.project.basePath,
            ).loadTasks(
                profile = profile,
                notify = false,
            )

        val tasksInFile = MiseConfigPsiUtils.getMiseTasks(root)
        val uniqueTasks = HashMap<String, MiseTask>()
        val currentPath =
            normalizePath(
                Path(root.project.basePath ?: "", root.viewProvider.virtualFile.path).toString(),
            )

        tasksFromMise
            .filter {
                when (it.source) {
                    null -> true
                    else -> normalizePath(it.source!!) != currentPath
                }
            }.forEach { uniqueTasks[it.name] = it }
        tasksInFile.forEach { uniqueTasks[it.name] = it }

        resultSet.addAllElements(
            uniqueTasks
                .filter { it.key.isNotBlank() }
                .map {
                    LookupElementBuilder
                        .create(it.key)
                        .withIcon(MiseIcons.DEFAULT)
                        .withTypeText(it.value.description ?: it.value.command)
                        .withCaseSensitivity(false)
                },
        )
    }
}
