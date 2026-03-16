package com.ua.yushchenko.f1.fastlaps.telemetry.processing.config;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.AbstractTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketHealthEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration.
 * Message value is a telemetry event entity ({@link AbstractTelemetryEvent} subclass:
 * {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent}, etc.) that includes a DTO payload.
 * Jackson deserializes to the correct event type via @JsonTypeInfo.
 * See: implementation_steps_plan.md § Етап 5.1.
 */
@Configuration
@EnableKafka
@EnableAsync
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.max-poll-records:100}")
    private Integer maxPollRecords;

    @Bean
    public ConsumerFactory<String, AbstractTelemetryEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        if (maxPollRecords != null && maxPollRecords > 0) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        }

        JsonDeserializer<AbstractTelemetryEvent> jsonDeserializer =
                new JsonDeserializer<>(AbstractTelemetryEvent.class, false)
                        .trustedPackages("com.ua.yushchenko.f1.fastlaps.telemetry.api.*");
        ErrorHandlingDeserializer<AbstractTelemetryEvent> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AbstractTelemetryEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AbstractTelemetryEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PacketHealthEvent> packetHealthConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<PacketHealthEvent> jsonDeserializer =
                new JsonDeserializer<>(PacketHealthEvent.class, false)
                        .trustedPackages("com.ua.yushchenko.f1.fastlaps.telemetry.api.*");
        ErrorHandlingDeserializer<PacketHealthEvent> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PacketHealthEvent> packetHealthContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PacketHealthEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(packetHealthConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
