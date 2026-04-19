package com.github.l34130.mise.core.command

import com.github.l34130.mise.core.command.serializers.FlexibleStringListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiseTask(
    val name: String,
    val aliases: List<String>? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val depends: List<List<String>>? = null,
    @SerialName("wait_for")
    @Serializable(with = FlexibleStringListSerializer::class)
    val waitFor: List<List<String>>? = null,
    @SerialName("depends_post")
    @Serializable(with = FlexibleStringListSerializer::class)
    val dependsPost: List<List<String>>? = null,
    val description: String? = null,
    val hide: Boolean = false,
    val source: String,
    val command: String? = null,
)
