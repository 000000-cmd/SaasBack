package com.saas.common.kafka;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion del producer Kafka. Crea el {@link KafkaTemplate} que usara el
 * {@link com.saas.common.outbox.OutboxRelay} para publicar mensajes.
 *
 * Se activa solo si {@code saas.kafka.enabled=true}. Esto permite apagar
 * Kafka en local cuando solo quieres trabajar con BD (los eventos se acumulan
 * en outbox como PENDING; cuando reactivas Kafka, el relay los recupera todos).
 *
 * Garantias de entrega configuradas:
 *
 *   acks=all: espera confirmacion de TODAS las replicas en ISR.
 *       Si el lider muere antes de replicar, no se confirma → no se pierde.
 *   idempotence=true: cada producer tiene un ID; el broker descarta
 *       duplicados de mensajes reintentados. Sin esto, retries duplican
 *       mensajes en Kafka.
 *   retries=MAX: reintentos infinitos hasta que delivery.timeout
 *       (120s) expira. Junto con idempotence, garantiza "exactly-once" desde
 *       el productor.
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "saas.kafka", name = "enabled", havingValue = "true")
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> p = new HashMap<>();

        // --- Conexion ---
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // --- Garantias de entrega ---
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        p.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);

        // --- Performance ---
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        p.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        p.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);

        return new DefaultKafkaProducerFactory<>(p);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }
}
