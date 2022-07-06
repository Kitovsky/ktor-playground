package com.lonerx.ktor

import com.lonerx.ktor.KafkaConfig.KAFKA_BOOTSTRAP_SERVERS
import com.lonerx.ktor.KafkaConfig.KAFKA_KEY_DESERIALIZER
import com.lonerx.ktor.KafkaConfig.KAFKA_VALUE_DESERIALIZER
import com.lonerx.ktor.KafkaConfig.POLL_DURATION_MILLIS
import com.lonerx.ktor.KafkaConfig.TOPIC
import com.lonerx.model.Fortune
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties
import java.util.UUID

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

            val fortuneReaderLoop = launch {
                application.log.debug("starting kafka reader loop")
                val consumer = createFortunesKafkaReceiver()
                consumer.subscribe(listOf(TOPIC))
                try {
                    while (isActive) {
                        consumer.poll(Duration.ofMillis(POLL_DURATION_MILLIS)).forEach {
                            val fortune = Json.decodeFromString<Fortune>(it.value())
                            application.log.debug("received fortune #${fortune.id}")
                            send(fortune.fortune)
                        }
                    }
                } finally {
                    application.log.debug("kafka reader loop done, cleaning resources")
                    consumer.apply {
                        unsubscribe()
                        close()
                    }
                }
            }

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            application.log.debug("Text frame received: $text")
                            send("echo: $text")
                            if (text.equals("bye", ignoreCase = true)) {
                                close(CloseReason(CloseReason.Codes.NORMAL, "Good bye!"))
                            }
                        }
                        is Frame.Binary -> {
                            application.log.debug("Binary frame received")
                        }
                        is Frame.Close -> {
                            application.log.debug("Close frame received")
                        }
                        is Frame.Ping -> {
                            application.log.debug("Ping frame received")
                        }
                        is Frame.Pong -> {
                            application.log.debug("Pong frame received")
                        }
                    }
                }
            } finally {
                application.log.debug("finally block: shutting down reader loop")
                fortuneReaderLoop.cancelAndJoin()
                application.log.debug("finally block: reader loop coroutine joined")
            }
        }
    }
}

fun createFortunesKafkaReceiver() = KafkaConsumer<String, String>(
    Properties().apply {
        put("group.id", UUID.randomUUID().toString())
        put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
        put("key.deserializer", KAFKA_KEY_DESERIALIZER)
        put("value.deserializer", KAFKA_VALUE_DESERIALIZER)
    }
)

object KafkaConfig {
    const val TOPIC = "lonerx.dev.fortunes"
    const val POLL_DURATION_MILLIS = 200L

    const val KAFKA_BOOTSTRAP_SERVERS = "kafka.local:9092"
    const val KAFKA_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
    const val KAFKA_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
}