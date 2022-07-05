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
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/test") {
            application.log.info("starting ws session")
            try {
                while (true) {
                    val delay = Random.nextInt(5, 15)
                    delay(delay.seconds)
                    outgoing.send(Frame.Text("delay was ${delay.seconds} seconds"))
                }
            } finally {
                application.log.info("closing ws session")
            }
        }
        webSocket("/ws/fortunes") {
            send("Welcome to Fortune Teller world!")
            application.log.info("starting ws session")
            val consumer = createFortunesKafkaReceiver()
            consumer.subscribe(listOf(TOPIC))
            try {
                application.log.debug("listening to kafka!!!")
                var num = 0
                while (num < 200) {
                    num++
                    application.log.debug("polling $num")
                    consumer.poll(Duration.ofMillis(POLL_DURATION_MILLIS)).forEach {
                        val fortune = Json.decodeFromString<Fortune>(it.value())
                        application.log.debug("received fortune #${fortune.id}")
                        send(fortune.fortune)
                    }
                }
            } finally {
                application.log.info("closing ws session")
                consumer.apply {
                    unsubscribe()
                    close()
                }
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
    const val POLL_DURATION_MILLIS = 500L

    const val KAFKA_BOOTSTRAP_SERVERS = "kafka.local:9092"
    const val KAFKA_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
    const val KAFKA_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
}