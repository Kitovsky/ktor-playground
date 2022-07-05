package com.lonerx.model

import kotlinx.serialization.Serializable

@Serializable
data class Fortune(
    val id: Int,
    val fortune: String
)
