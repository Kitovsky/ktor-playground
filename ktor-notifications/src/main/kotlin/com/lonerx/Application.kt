package com.lonerx

import com.lonerx.ktor.events.PersonEvent
import io.ktor.server.application.*
import com.lonerx.ktor.routing.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {

    environment.monitor.subscribe(PersonEvent) { person ->
        log.debug("PersonEvent received: $person")
    }

    configureRouting()
}
