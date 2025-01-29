package com.github.l34130.mise.core.lang.psi

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.childrenOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.TomlValue
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

@RequiresReadLock
fun MiseTomlFile.allTasks(): Sequence<TomlKeySegment> {
    val explicitTasks = hashSetOf<String>()

    return childrenOfType<TomlTable>()
        .asSequence()
        .flatMap { table ->
            val header = table.header
            when {
                // [tasks.<task-name>]
                header.isSpecificTaskTableHeader -> {
                    val lastKey = header.key?.segments?.last()
                    if (lastKey != null && lastKey.name !in explicitTasks) {
                        sequenceOf(lastKey)
                    } else {
                        emptySequence()
                    }
                }
                else -> emptySequence()
            }
        }.constrainOnce()
}

@RequiresReadLock
fun MiseTomlFile.resolveTask(taskName: String): Sequence<ResolveResult> =
    allTasks()
        .filter { it.name == taskName }
        .map { PsiElementResolveResult(it) }

@get:RequiresReadLock
val TomlTable.taskName: String?
    get() {
        if (header.isSpecificTaskTableHeader) {
            val headerKey = header.key ?: return null
            return headerKey.segments.lastOrNull()?.name
        }
        return null
    }

@get:RequiresReadLock
val TomlTableHeader.miseTomlTask: TomlKeySegment?
    get() {
        if (isSpecificTaskTableHeader) {
            val headerKey = key ?: return null
            return headerKey.segments.lastOrNull()
        }
        return null
    }

@get:RequiresReadLock
val TomlTableHeader.isSpecificTaskTableHeader: Boolean
    get() {
        val names = key?.segments.orEmpty()
        return names.getOrNull(names.size - 2)?.name == "tasks"
    }

@RequiresReadLock
fun TomlKeyValueOwner.getValueWithKey(key: String): TomlValue? = entries.find { it.key.text == key }?.value

@get:RequiresReadLock
val TomlValue.stringValue: String?
    get() {
        val kind = (this as? TomlLiteral)?.kind
        return (kind as? TomlLiteralKind.String)?.value
    }
