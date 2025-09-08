package com.github.l34130.mise.core.command

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

object MiseCommandLineOutputParser {
    inline fun <reified T> parse(output: String): T = parse(output, jacksonTypeRef<T>())

    fun <T> parse(
        output: String,
        typeReference: TypeReference<T>,
    ): T = OBJECT_MAPPER.readValue(output, typeReference)

    private val OBJECT_MAPPER =
        jsonMapper {
            addModule(kotlinModule())
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        }
}
