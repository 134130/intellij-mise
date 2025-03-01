package com.github.l34130.mise.core.model

import com.github.l34130.mise.core.lang.psi.MiseTomlPsiPatterns
import com.github.l34130.mise.core.lang.psi.getValueWithKey
import com.github.l34130.mise.core.lang.psi.or
import com.github.l34130.mise.core.lang.psi.stringArray
import com.github.l34130.mise.core.lang.psi.stringValue
import com.github.l34130.mise.core.util.AbsolutePath
import com.github.l34130.mise.core.util.RelativePath
import com.github.l34130.mise.core.util.baseDirectory
import com.github.l34130.mise.core.util.collapsePath
import com.github.l34130.mise.core.util.getRelativePath
import com.intellij.execution.PsiLocation
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.containers.addIfNotNull
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import kotlin.collections.component1
import kotlin.collections.component2

sealed interface MiseTask {
    val name: String
    val aliases: List<String>?
    val depends: List<String>?
    val waitFor: List<String>?
    val dependsPost: List<String>?
    val description: String?

    @RelativePath
    val source: String?

    companion object {
        val DATA_KEY = DataKey.create<MiseTask>(javaClass.simpleName)
    }
}

fun MiseTask.psiLocation(project: Project): PsiLocation<*>? =
    when (this) {
        is MiseShellScriptTask -> {
            val psiFile = this.file.findPsiFile(project) ?: return null
            PsiLocation(psiFile)
        }
        is MiseTomlTableTask -> PsiLocation(this.keySegment)
        is MiseUnknownTask -> {
            val source = this.source ?: return null
            val file = LocalFileSystem.getInstance().findFileByPath(source) ?: return null
            val psiFile = file.findPsiFile(project) ?: return null
            PsiLocation(psiFile)
        }
    }

class MiseUnknownTask internal constructor(
    override val name: String,
    override val aliases: List<String>? = null,
    override val depends: List<String>? = null,
    override val waitFor: List<String>? = null,
    override val dependsPost: List<String>? = null,
    override val description: String? = null,
    @AbsolutePath
    override val source: String? = null,
) : MiseTask

class MiseShellScriptTask internal constructor(
    override val name: String,
    override val aliases: List<String>? = null,
    override val depends: List<String>? = null,
    override val waitFor: List<String>? = null,
    override val dependsPost: List<String>? = null,
    override val description: String? = null,
    override val source: String? = null,
    val file: VirtualFile,
) : MiseTask {
    companion object {
        fun resolveOrNull(
            project: Project,
            baseDir: VirtualFile,
            file: VirtualFile,
        ): MiseShellScriptTask? =
            MiseShellScriptTask(
                name = FileUtil.splitPath(getRelativePath(baseDir, file)!!.substringBeforeLast('.')).joinToString(":"),
                file = file,
                source = getRelativePath(project.baseDirectory(), file.path),
            )
    }
}

class MiseTomlTableTask internal constructor(
    override val name: String,
    override val aliases: List<String>? = null,
    override val depends: List<String>? = null,
    override val waitFor: List<String>? = null,
    override val dependsPost: List<String>? = null,
    override val description: String? = null,
    override val source: String? = null,
    val keySegment: TomlKeySegment,
) : MiseTask {
    companion object {
        private val acceptPattern =
            MiseTomlPsiPatterns.miseTomlStringLiteral or
                MiseTomlPsiPatterns.miseTomlLeafPsiElement or
                MiseTomlPsiPatterns.tomlPsiElement<TomlKeySegment>()

        fun resolveAllFromTomlFile(file: TomlFile): List<MiseTomlTableTask> {
            val result = mutableListOf<MiseTomlTableTask>()

            val tables = file.childrenOfType<TomlTable>()
            for (table in tables) {
                val keySegments = table.header.key?.segments ?: continue
                when (keySegments.size) {
                    1 -> {
                        // [tasks]
                        // foo = {  }
                        if (keySegments.first().textMatches("tasks")) {
                            val keyValues = table.childrenOfType<TomlKeyValue>()
                            for (keyValue in keyValues) {
                                val keySegment = keyValue.key.segments.singleOrNull() ?: continue
                                result.addIfNotNull(
                                    resolveFromInlineTableInTaskTable(keySegment),
                                )
                            }
                        }
                    }
                    2 -> {
                        // [tasks.foo]
                        val (first, second) = keySegments
                        if (first.textMatches("tasks")) {
                            result.addIfNotNull(resolveFromTaskChainedTable(second))
                        }
                    }
                    else -> continue
                }
            }

            return result
        }

        /**
         * If the given [psiElement] is a [TomlKeySegment] or [TomlLiteral] of a task table, returns the [MiseTomlTableTask] instance.
         *
         *   ```toml
         *   [tasks.<foo>]
         *   run = "echo foo"
         *   ```
         */
        fun resolveFromTaskChainedTable(psiElement: PsiElement): MiseTomlTableTask? {
            if (!acceptPattern.accepts(psiElement)) return null
            val tomlKey =
                if (psiElement is TomlKeySegment) {
                    psiElement.parent as? TomlKey
                } else {
                    psiElement.parent.parent as? TomlKey
                } ?: return null

            val tomlTableHeader = tomlKey.parent as? TomlTableHeader ?: return null
            val keySegments = tomlTableHeader.key?.segments ?: return null
            if (keySegments.size != 2) return null
            if (keySegments[0].name != "tasks") return null
            if (keySegments[0] == psiElement.parent) return null // escape self (tasks)

            val table = tomlTableHeader.parent as? TomlTable ?: return null
            val keySegment = keySegments[1]
            return MiseTomlTableTask(
                name = keySegment.name ?: return null,
                description = table.getValueWithKey("description")?.stringValue,
                depends = table.getValueWithKey("depends")?.stringArray,
                waitFor = table.getValueWithKey("wait_for")?.stringArray,
                dependsPost = table.getValueWithKey("depends_post")?.stringArray,
                aliases = table.getValueWithKey("alias")?.stringArray,
                source = collapsePath(psiElement.containingFile, psiElement.project),
                keySegment = keySegment,
            )
        }

        /**
         * If the given [psiElement] is a key segment(or literal) of a task table, returns the [MiseTomlTableTask] instance.
         *
         *   ```toml
         *   [tasks]
         *   <foo> = { run = "echo foo" }
         *   ```
         */
        fun resolveFromInlineTableInTaskTable(psiElement: PsiElement): MiseTomlTableTask? {
            val tomlKey =
                if (psiElement is TomlKeySegment) {
                    psiElement.parent as? TomlKey
                } else {
                    psiElement.parent.parent as? TomlKey
                } ?: return null

            val tomlTable = tomlKey.parent.parent as? TomlTable ?: return null
            val tomlTableHeader = tomlTable.header
            if (tomlTableHeader == tomlKey.parent) return null // escape self (tasks)
            @Suppress("ktlint:standard:chain-method-continuation")
            if (tomlTableHeader.key?.segments?.singleOrNull()?.textMatches("tasks") != true) {
                return null
            }

            val keySegment = tomlKey.segments.singleOrNull() ?: return null
            val table = (tomlKey.parent as? TomlKeyValue)?.value as? TomlInlineTable ?: return null
            return MiseTomlTableTask(
                name = keySegment.name ?: return null,
                description = table.getValueWithKey("description")?.stringValue,
                depends = table.getValueWithKey("depends")?.stringArray,
                waitFor = table.getValueWithKey("wait_for")?.stringArray,
                dependsPost = table.getValueWithKey("depends_post")?.stringArray,
                aliases = table.getValueWithKey("alias")?.stringArray,
                source = collapsePath(psiElement.containingFile, psiElement.project),
                keySegment = keySegment,
            )
        }

        /**
         * If the given [psiElement] is a [TomlKeySegment] or [TomlLiteral] of a task table, returns the [MiseTomlTableTask] instance.
         *
         * This method is for `tasks.toml` file.
         */
        fun resolveOrNull(psiElement: PsiElement): MiseTomlTableTask? {
            if (!acceptPattern.accepts(psiElement)) return null
            val tomlKey =
                if (psiElement is TomlKeySegment) {
                    psiElement.parent as? TomlKey
                } else {
                    psiElement.parent.parent as? TomlKey
                } ?: return null

            val table = tomlKey.parent.parent as? TomlTable ?: return null
            val keySegment = tomlKey.segments.singleOrNull() ?: return null

            return MiseTomlTableTask(
                name = keySegment.name ?: return null,
                description = table.getValueWithKey("description")?.stringValue,
                depends = table.getValueWithKey("depends")?.stringArray,
                waitFor = table.getValueWithKey("wait_for")?.stringArray,
                dependsPost = table.getValueWithKey("depends_post")?.stringArray,
                aliases = table.getValueWithKey("alias")?.stringArray,
                source = collapsePath(psiElement.containingFile, psiElement.project),
                keySegment = keySegment,
            )
        }
    }
}
