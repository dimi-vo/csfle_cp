import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.logging.log4j.kotlin.logger
import java.util.*

class ProducerProperties {

    private val logger = logger(javaClass.name)

    fun configureProperties(): Properties {

        val settings = Properties()
        settings.setProperty(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringSerializer"
        )
        settings.setProperty(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            "io.confluent.kafka.serializers.KafkaAvroSerializer"
        )
        settings.setProperty("security.protocol", "SASL_SSL")
        settings.setProperty("sasl.mechanism", "PLAIN")
        settings.setProperty("schema.registry.url", "http://localhost:8081")
        // Required since we manually create schemas
        settings.setProperty("use.latest.version", "true")
        settings.setProperty("auto.register.schemas", "false")
        //TODO: Provide the correct Bootstrap URL
        settings.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "pkc-<env>.eu-central-1.aws.confluent.cloud:9092")
        //TODO: Provide the API KEY and SECRET
        settings.setProperty(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required username='API-KEY' password='API-SECRET';"
        )

        settings.stringPropertyNames()
            .associateWith { settings.getProperty(it) }
            .forEach { logger.info(String.format("%s", it)) }
        return settings
    }
}
