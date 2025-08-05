import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

class ConsumerProperties {

    fun configureProperties(): Properties {

        val settings = Properties()
        settings.setProperty(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer"
        )
        settings.setProperty(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "io.confluent.kafka.serializers.KafkaAvroDeserializer"
        )
        val consumerGroup: String = System.getenv("TARGET_CONSUMER_GROUP") ?: "<Consumer Group>"
        settings.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
        settings.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        settings.setProperty("sasl.mechanism", "PLAIN")
        settings.setProperty("schema.registry.url", "http://localhost:8081")
        settings.setProperty("security.protocol", "SASL_SSL")
        //TODO: Provide the correct Bootstrap URL
        settings.setProperty(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "pkc-<envID>.eu-central-1.aws.confluent.cloud:9092"
        )
        //TODO: Provide the API KEY and SECRET
        settings.setProperty(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required username='API-KEY' password='API-SECRET';"
        )

        return settings
    }
}
