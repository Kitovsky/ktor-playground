package com.lonerx.ktor.routing

import com.lonerx.ktor.events.Person
import com.lonerx.ktor.events.PersonEvent
import io.ktor.server.application.Application
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

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
