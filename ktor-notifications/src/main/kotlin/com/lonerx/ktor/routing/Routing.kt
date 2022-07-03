package com.lonerx.ktor.routing

import com.lonerx.ktor.events.Person
import com.lonerx.ktor.events.PersonEvent
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!\n")
        }

        get("/person") {
            val firstName = call.request.queryParameters["firstName"] ?: "John"
            val lastName = call.request.queryParameters["lastName"] ?: "Doe"

            this.application.environment.monitor.raise(PersonEvent, Person(firstName, lastName))

            call.respondText { "$firstName $lastName\n" }
        }
    }
}
