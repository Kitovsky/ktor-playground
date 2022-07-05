package com.lonerx.ktor

import com.lonerx.ktor.KafkaConfig.KAFKA_BOOTSTRAP_SERVERS
import com.lonerx.ktor.KafkaConfig.KAFKA_KEY_SERIALIZER
import com.lonerx.ktor.KafkaConfig.KAFKA_VALUE_SERIALIZER
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
import io.ktor.websocket.Frame
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/fortunes") {
            application.log.info("starting ws session")
            val receiver = createFortunesKafkaReceiver()
            receiver.subscribe(listOf(TOPIC))
            try {
                while (true) {
                    val records = receiver.poll(Duration.ofMillis(POLL_DURATION_MILLIS))
                    for (record in records) {
                        val fortune = Json.decodeFromString<Fortune>(record.value())
                        application.log.debug("received fortune #${fortune.id}")
                        outgoing.send(Frame.Text(fortune.fortune))
                    }
                }
            } finally {
                application.log.info("closing ws session")
                receiver.unsubscribe()
                receiver.close()
            }
        }
    }
}

fun createFortunesKafkaReceiver() = KafkaConsumer<String, String>(
    Properties().apply {
        put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
        put("key.serializer", KAFKA_KEY_SERIALIZER)
        put("value.serializer", KAFKA_VALUE_SERIALIZER)
    }
)

object KafkaConfig {
    const val TOPIC = "lonerx.dev.fortunes"
    const val POLL_DURATION_MILLIS = 100L

    const val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
    const val KAFKA_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
    const val KAFKA_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
}