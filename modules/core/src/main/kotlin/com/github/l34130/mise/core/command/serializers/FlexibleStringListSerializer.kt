package com.github.l34130.mise.core.command.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mise returns `depends`/`wait_for`/`depends_post` in several shapes depending on how the
 * task is declared in `mise.toml`:
 *   - Scalar string: `"echo"`
 *   - Array of strings (one dep per entry): `["echo", "foo"]`
 *   - Array of arrays (shell-split args): `[["echo", "'Hello", "World'"]]`
 *
 * We normalize every shape to `List<List<String>>`, matching Jackson's previous
 * `ACCEPT_SINGLE_VALUE_AS_ARRAY` behavior (each scalar becomes a one-element list).
 */
object FlexibleStringListSerializer : KSerializer<List<List<String>>> {
    private val delegate = ListSerializer(ListSerializer(String.serializer()))

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<List<String>> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("FlexibleStringListSerializer only supports JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> listOf(listOf(element.content))
            is JsonArray -> element.map { item ->
                when (item) {
                    is JsonArray -> item.map { it.jsonPrimitive.content }
                    is JsonPrimitive -> listOf(item.content)
                    else -> throw SerializationException("Unexpected element in flexible list: $item")
                }
            }
            else -> throw SerializationException("Unexpected element for flexible list: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: List<List<String>>) {
        encoder.encodeSerializableValue(delegate, value)
    }
}
