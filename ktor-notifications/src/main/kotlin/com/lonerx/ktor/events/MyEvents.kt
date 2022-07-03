package com.lonerx.ktor.events

import io.ktor.events.EventDefinition

data class Person(
    val firstName: String,
    val lastName: String
)

val PersonEvent: EventDefinition<Person> = EventDefinition()

