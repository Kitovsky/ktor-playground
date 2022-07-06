package com.lonerx.fortunes

import kotlinx.serialization.Serializable

@Serializable
data class Fortune(
    val id: Int,
    val text: String
)
