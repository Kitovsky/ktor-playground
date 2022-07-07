package com.lonerx.ktor

import com.lonerx.FortuneSessionHandler
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import java.time.Duration

@Suppress("LongMethod")
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/fortunes") {
            application.log.info("ws session started")
            send("Welcome to fortunes world!")
            send("Any text you enter will be echoed, enter 'bye' to quit")

//            val sessionJob = FortuneSessionHandler(this).start()
//            application.log.debug("session handler started")
//            // do some useful stuff here
//            sessionJob.join()
//            application.log.debug("session handler finished")

            application.log.debug("starting session handler")
            FortuneSessionHandler(application.environment.config, this).startBlocking()
            application.log.debug("session handler finished")
        }
    }
}
