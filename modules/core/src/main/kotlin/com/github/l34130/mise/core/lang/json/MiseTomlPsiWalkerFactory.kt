package com.github.l34130.mise.core.lang.json

import com.github.l34130.mise.core.lang.MiseTomlLanguage
import com.intellij.psi.PsiElement
import com.jetbrains.jsonSchema.extension.JsonLikePsiWalker
import com.jetbrains.jsonSchema.extension.JsonLikePsiWalkerFactory
import com.jetbrains.jsonSchema.impl.JsonSchemaObject
import org.toml.ide.json.TomlJsonPsiWalker

class MiseTomlPsiWalkerFactory : JsonLikePsiWalkerFactory {
    override fun handles(element: PsiElement): Boolean = element.containingFile.language is MiseTomlLanguage

    override fun create(schemaObject: JsonSchemaObject): JsonLikePsiWalker = TomlJsonPsiWalker
}
