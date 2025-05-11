import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.logging.log4j.kotlin.logger
import java.util.*
import java.lang.System;

class ProducerProperties {

    private val logger = logger(javaClass.name)

    fun configureProperties() : Properties{

        val settings = Properties()
        settings.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        settings.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer")

        val broker_boostrap: String = "localhost:9091"
        val schema_registry_url: String = "http://localhost:8081"

        settings.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "$broker_boostrap")

        settings.setProperty("schema.registry.url", "$schema_registry_url")

        // Required since we manually create schemas
        settings.setProperty("use.latest.version", "true")
        settings.setProperty("auto.register.schemas","false")


        settings.stringPropertyNames()
                .associateWith {settings.getProperty(it)}
                .forEach { logger.info(String.format("%s", it)) }
        return settings
    }
}