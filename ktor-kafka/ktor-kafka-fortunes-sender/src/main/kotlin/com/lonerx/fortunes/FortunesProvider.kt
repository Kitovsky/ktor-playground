package com.lonerx.fortunes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KLogging

@OptIn(ExperimentalSerializationApi::class)
class FortunesProvider : KLogging() {
    private val fortunes: List<Fortune>

    init {
        fortunes = Json.decodeFromStream(getResourceAsInputStream("/fortunes.json"))
        logger.debug { "${fortunes.size} fortunes found" }
    }

    fun nextFortune() = fortunes.random()

    @Suppress("SameParameterValue")
    private fun getResourceAsInputStream(path: String) =
        object {}.javaClass.getResourceAsStream(path) ?: throw java.lang.IllegalArgumentException("file $path not found in resources")
}