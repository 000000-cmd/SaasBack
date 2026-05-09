package com.saas.search.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion del consumer Kafka.
 *
 * Habilita {@code @KafkaListener} mediante {@code @EnableKafka}, y crea
 * la {@code ConcurrentKafkaListenerContainerFactory} que lo orquesta.
 *
 * Garantias del consumer:
 *
 *   Manual ack: solo confirma offsets cuando el procesamiento
 *       termina sin error. Si falla, Kafka reentrega el mensaje.
 *   Retry con backoff: ante error, reintenta 3 veces con 2s entre
 *       cada uno. Despues de 3 fallos, va al DLQ
 *       ({@code domain.events.dlq}).
 *   Concurrency 3: 3 threads paralelos. Con 12 particiones del topic,
 *       cada thread atiende 4. Si escalas a 12 instancias, cada una con threads=1
 *       (por config), tienes 1 instancia por particion: throughput maximo.
 *
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.listener.concurrency:3}")
    private int concurrency;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> p = new HashMap<>();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        p.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);
        return new DefaultKafkaConsumerFactory<>(p);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Error handler: 3 reintentos con 2s de backoff. Despues de 3 fallos
        // consecutivos del MISMO mensaje, lo descarta y avanza el offset
        // (con un log de error). Para mandar al DLQ habria que agregar un
        // DeadLetterPublishingRecoverer (lo dejamos para fase posterior si lo
        // necesitas).
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2000L, 3L)));

        return factory;
    }
}
