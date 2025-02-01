package com.github.l34130.mise.core.lang.psi

import com.github.l34130.mise.core.model.MiseTask
import com.intellij.psi.util.childrenOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.TomlValue
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind
import kotlin.collections.getOrNull
import kotlin.collections.orEmpty

@RequiresReadLock
fun TomlFile.allTasks(): Sequence<MiseTask.TomlTable> =
    sequence {
        val tables = childrenOfType<TomlTable>()
        for (table in tables) {
            val headerKeySegments = table.header.key?.segments ?: continue

            when (headerKeySegments.size) {
                1 -> {
                    // [tasks]
                    // foo = {  }
                    if (headerKeySegments.first().textMatches("tasks")) {
                        val keyValues = table.childrenOfType<TomlKeyValue>()
                        for (keyValue in keyValues) {
                            val key = keyValue.key
                            if (key.segments.size == 1) {
                                yield(MiseTask.TomlTable.resolveOrNull(key.segments.first()))
                            }
                        }
                    }
                }
                2 -> {
                    // [tasks.foo]
                    val (first, second) = headerKeySegments
                    if (first.textMatches("tasks")) {
                        yield(MiseTask.TomlTable.resolveOrNull(second))
                    }
                }
                else -> continue
            }
        }
    }.filterNotNull().constrainOnce()

@get:RequiresReadLock
val TomlTable.taskName: String?
    get() {
        if (header.isSpecificTaskTableHeader) {
            val headerKey = header.key ?: return null
            return headerKey.segments.lastOrNull()?.name
        }
        return null
    }

/**
 * ```
 * [tasks.foo]
 * ```
 */
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

@get:RequiresReadLock
val TomlValue.stringArray: List<String>?
    get() {
        return when (this) {
            is TomlLiteral -> stringValue?.let { listOf(it) }
            is TomlArray -> elements.mapNotNull { it.stringValue }
            else -> null
        }
    }
