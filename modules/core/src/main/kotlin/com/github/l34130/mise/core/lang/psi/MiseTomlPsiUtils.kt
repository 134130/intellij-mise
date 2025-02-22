package com.github.l34130.mise.core.lang.psi

import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlValue
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

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
