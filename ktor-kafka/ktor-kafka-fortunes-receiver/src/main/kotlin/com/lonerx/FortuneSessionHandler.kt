package com.lonerx

import com.lonerx.model.Fortune
import io.ktor.server.config.ApplicationConfig
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties
import java.util.UUID

class FortuneSessionHandler(
    config: ApplicationConfig,
    private val session: WebSocketSession
) : CoroutineScope, KLogging() {

    private val supervisor = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + supervisor + CoroutineExceptionHandler { _, t ->
        logger.error(t) { "An error occurred during coroutine execution, stopping everything" }
        supervisor.cancel()
    }

    private val bootstrapServers = config.property("kafka.bootstrap.servers").getList()
    private val topicName = config.property("kafka.topic.name").getString()

    private val incoming = session.incoming
    private val outgoing = session.outgoing

    @Suppress("MemberVisibilityCanBePrivate")
    fun start(): CompletableJob {
        launch {
            echoLoop()
        }
        launch {
            kafkaLoop()
        }
        return supervisor
    }

    suspend fun startBlocking() = start().join()

    private suspend fun kafkaLoop() {
        val consumer = createKafkaConsumer()
        consumer.subscribe(listOf(topicName))
        try {
            while (isActive) {
                consumer.poll(Duration.ofMillis(POLL_DURATION_MILLIS)).forEach { record ->
                    val fortune = Json.decodeFromString<Fortune>(record.value())
                    logger.debug { "kafkaLoop: received fortune #${fortune.id}" }
                    outgoing.send(Frame.Text("FORTUNE #${fortune.id}:\n${fortune.text}\n"))
                }
            }
        } finally {
            logger.debug { "kafkaLoop: cleaning" }
            consumer.unsubscribe()
            consumer.close()
            logger.debug { "kafkaLoop: stopping supervisor" }
            supervisor.cancel()
        }
    }

    private suspend fun echoLoop() {
        try {
            for (frame in incoming) {
                parseIncomingFrame(frame)
            }
        } finally {
            logger.debug { "echoLoop: stopping supervisor" }
            supervisor.cancel()
        }
    }

    private suspend fun parseIncomingFrame(frame: Frame) {
        when (frame) {
            // If frame is text, we just echo it back to client
            // If text is 'bye', we close websockets session
            is Frame.Text -> {
                val text = frame.readText()
                logger.debug("Text frame received: $text, echo back to client")
                outgoing.send(Frame.Text("YOU WROTE: $text"))
                if (text.equals("bye", ignoreCase = true)) {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "Good bye!"))
                }
            }
            // For other types of frames we just log the fact of their appearance
            is Frame.Binary -> {
                logger.debug("Binary frame received")
            }
            is Frame.Close -> {
                logger.debug("Close frame received")
            }
            is Frame.Ping -> {
                logger.debug("Ping frame received")
            }
            is Frame.Pong -> {
                logger.debug("Pong frame received")
            }
        }
    }

    private fun createKafkaConsumer() = KafkaConsumer<String, String>(
        Properties().apply {
            put("group.id", UUID.randomUUID().toString())
            put("bootstrap.servers", bootstrapServers)
            put("key.deserializer", KAFKA_KEY_DESERIALIZER)
            put("value.deserializer", KAFKA_VALUE_DESERIALIZER)
        }
    )

    companion object {
        const val POLL_DURATION_MILLIS = 200L
        const val KAFKA_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
        const val KAFKA_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer"
    }
}