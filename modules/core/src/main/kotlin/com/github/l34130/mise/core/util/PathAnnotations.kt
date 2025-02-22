package com.github.l34130.mise.core.util

/**
 * Annotates a field or property that represents an absolute path.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class AbsolutePath

/**
 * Annotates a field or property that represents a relative path.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class RelativePath
