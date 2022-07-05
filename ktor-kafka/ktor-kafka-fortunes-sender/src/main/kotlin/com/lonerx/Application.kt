package com.lonerx

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.koin

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    koin {
        modules(
            fortunesProvider
        )
    }

    routing {
        get("/") {
            call.respondText("fortune teller is up and running\n")
        }
    }
}
