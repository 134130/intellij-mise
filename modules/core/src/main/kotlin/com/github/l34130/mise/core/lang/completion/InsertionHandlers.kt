package com.github.l34130.mise.core.lang.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.toml.lang.psi.TomlElementTypes
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.ext.elementType

class StringLiteralInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(
        context: InsertionContext,
        item: LookupElement,
    ) {
        val leaf = context.getElementOfType<PsiElement>() ?: return
        val elementType = (leaf.parent as? TomlLiteral)?.elementType

        val hasQuotes = elementType in tokenSet
        if (!hasQuotes) {
            context.document.insertString(context.startOffset, "\"")
            context.document.insertString(context.selectionEndOffset, "\"")
        }
    }

    private val tokenSet =
        TokenSet.create(
            TomlElementTypes.BASIC_STRING,
            TomlElementTypes.LITERAL_STRING,
            TomlElementTypes.LITERAL,
        )
}

inline fun <reified T : PsiElement> InsertionContext.getElementOfType(strict: Boolean = false): T? =
    PsiTreeUtil.findElementOfClassAtOffset(file, tailOffset - 1, T::class.java, strict)
