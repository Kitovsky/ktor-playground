package com.lonerx

import com.lonerx.ktor.configureWebSockets
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureWebSockets()
    routing {
        get("/") {
            call.respondText("fortune receiver is up and running\n")
        }
    }
}