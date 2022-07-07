package com.lonerx.fortunes

import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.TopicConfig
import java.util.Properties
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class FortunesKafkaPublisher(
    config: ApplicationConfig,
    private val provider: FortunesProvider
) : CoroutineScope, KLogging() {

    private val supervisor = SupervisorJob()
    override val coroutineContext = Dispatchers.Default + supervisor + CoroutineExceptionHandler { _, t ->
        logger.error(t) { "An error occurred during sender coroutine execution, restarting loop" }
        startSendingLoop()
    }

    private val kafkaProducer: KafkaProducer<String, String>
    private val topicName: String

    init {
        topicName = config.property("kafka.topic.name").getString()
        val topicPartitions = config.property("kafka.topic.partitions").getString().toInt()
        val topicReplicas = config.property("kafka.topic.replicas").getString().toShort()

        val bootstrapServers = config.property("kafka.bootstrap.servers").getList()

        val producerProperties = Properties().apply {
            put("bootstrap.servers", bootstrapServers)
            put("key.serializer", KAFKA_KEY_SERIALIZER)
            put("value.serializer", KAFKA_VALUE_SERIALIZER)
        }
        kafkaProducer = KafkaProducer(producerProperties)

        val adminProperties = Properties().apply {
            put("bootstrap.servers", bootstrapServers)
        }

        AdminClient.create(adminProperties).use { admin ->
            if (admin.listTopics().names().get().contains(topicName)) {
                logger.debug { "topic $topicName already exists" }
            } else {
                logger.debug { "topic $topicName does not exist, creating it now" }
                // createTopics() is async, hence we wait for completion with .get()
                admin.createTopics(
                    listOf(
                        NewTopic(topicName, topicPartitions, topicReplicas).configs(
                            mapOf(TopicConfig.RETENTION_MS_CONFIG to RECORD_RETENTION_TIME_MS.toString())
                        )
                    )
                ).values()[topicName]?.get()
                logger.debug { "topic $topicName has been created" }
            }
        }

        startSendingLoop()
    }

    private fun startSendingLoop() {
        logger.debug { "starting fortune sending loop" }
        launch {
            while (isActive) {
                val delay = Random.nextInt(INTERVAL_MIN_SEC, INTERVAL_MAX_SEC)
                delay(delay.seconds)
                val fortune = provider.nextFortune()
                logger.debug { "sending fortune #${fortune.id}" }
                kafkaProducer.send(ProducerRecord(topicName, "FORTUNE", Json.encodeToString(fortune)))
            }
        }
    }

    companion object {
        const val RECORD_RETENTION_TIME_MS = 5 * 1000

        const val INTERVAL_MIN_SEC = 5
        const val INTERVAL_MAX_SEC = 15

        const val KAFKA_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
        const val KAFKA_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
    }
}