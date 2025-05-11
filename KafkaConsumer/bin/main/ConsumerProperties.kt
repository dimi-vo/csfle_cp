import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import java.util.*

class ConsumerProperties {

    fun configureProperties() : Properties{

        val settings = Properties()
        settings.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
        settings.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer")
        val consumer_group: String = System.getenv("TARGET_CONSUMER_GROUP") ?: "<Consumer Group>"
        settings.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "$consumer_group")
        settings.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

        val broker_boostrap: String = "localhost:9091"
        val schema_registry_url: String = "http://localhost:8081"
       
        settings.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "$broker_boostrap")
        settings.setProperty("schema.registry.url", "$schema_registry_url")


        return settings
    }
}