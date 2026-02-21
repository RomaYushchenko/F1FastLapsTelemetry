package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka implementation of {@link TelemetryPublisher}.
 * Simple delegation to KafkaTemplate without retry or throttling
 * (those concerns are handled by decorators).
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaTelemetryPublisher implements TelemetryPublisher {

    private static final org.slf4j.Logger OUTBOUND_LOG = LoggerFactory.getLogger("outbound-events");

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object value) {
        OUTBOUND_LOG.debug("Publishing message to topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, value);
    }
}
