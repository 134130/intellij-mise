package com.github.l34130.mise.core.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.Reader

inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(json: Reader) = fromJson<T>(json, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(reader: JsonReader) = fromJson<T>(reader, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(json: JsonElement) = fromJson<T>(json, object : TypeToken<T>() {}.type)
