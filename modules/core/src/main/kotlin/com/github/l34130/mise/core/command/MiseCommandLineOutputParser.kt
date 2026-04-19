package com.github.l34130.mise.core.command

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.serializer

object MiseCommandLineOutputParser {
    inline fun <reified T> parse(output: String): T = JSON.decodeFromString(serializer<T>(), output)

    fun <T> parse(output: String, deserializer: DeserializationStrategy<T>): T =
        JSON.decodeFromString(deserializer, output)

    @OptIn(ExperimentalSerializationApi::class)
    val JSON = Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        coerceInputValues = true
    }
}
