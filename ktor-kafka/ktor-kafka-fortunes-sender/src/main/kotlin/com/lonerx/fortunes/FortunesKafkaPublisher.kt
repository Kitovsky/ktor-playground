package com.lonerx.fortunes

import com.lonerx.fortunesProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import java.util.Properties
import java.util.concurrent.ExecutionException
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class FortunesKafkaPublisher(
    private val provider: FortunesProvider
) : CoroutineScope, KLogging() {

    private val supervisor = SupervisorJob()
    override val coroutineContext = Dispatchers.Default + supervisor + CoroutineExceptionHandler { _, t ->
        logger.error(t) { "An error occurred during sender coroutine execution, restarting loop" }
        startSendingLoop()
    }

    private val kafkaProducer: KafkaProducer<String, String>
    private val topic: NewTopic

    init {
        val producerConfig = Properties().apply {
            put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
            put("key.serializer", KAFKA_KEY_SERIALIZER)
            put("value.serializer", KAFKA_VALUE_SERIALIZER)
        }
        kafkaProducer = KafkaProducer(producerConfig)

        val adminConfig = Properties().apply {
            put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
        }
        val kafkaAdmin = AdminClient.create(adminConfig)
        try {
            val res = kafkaAdmin.describeTopics(listOf(TOPIC))
            val descr = res.topicNameValues()[TOPIC]?.get()
            logger.debug { "topic $TOPIC already exists: $descr" }
        } catch (e: ExecutionException) {
            if (e.cause !is UnknownTopicOrPartitionException) {
                logger.error(e) { "exception" }
                throw e
            }

            logger.debug { "topic $TOPIC does not exist, creating it now" }
            val newTopicConfig = mapOf(
                TopicConfig.RETENTION_MS_CONFIG to RECORD_RETENTION_TIME_MS.toString()
            )
            val newTopic = NewTopic(TOPIC, PARTITIONS, REPLICAS).configs(newTopicConfig)
            kafkaAdmin.createTopics(listOf(newTopic)).values()[TOPIC]?.get()
            logger.debug { "topic $TOPIC has been created" }
        } finally {
            kafkaAdmin.close()
        }

        topic = NewTopic(TOPIC, PARTITIONS, REPLICAS)
    }

    private fun startSendingLoop() {
        logger.debug { "starting fortune teller sending loop" }
        launch {
            while (isActive) {
                val delay = Random.nextInt(INTERVAL_MIN_SEC, INTERVAL_MAX_SEC)
                delay(delay.seconds)
                val fortune = provider.nextFortune()
                logger.debug { "sending fortune #${fortune.id}" }
                kafkaProducer.send(ProducerRecord("FORTUNE", fortune.fortune))
            }
        }
    }

    companion object {
        const val TOPIC = "lonerx.dev.fortunes"
        const val PARTITIONS = 3
        const val REPLICAS: Short = 1
        const val RECORD_RETENTION_TIME_MS = 3 * 1000

        const val INTERVAL_MIN_SEC = 5
        const val INTERVAL_MAX_SEC = 15

        const val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
        const val KAFKA_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
        const val KAFKA_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer"
    }
}