package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

/**
 * Interface for publishing telemetry data to external systems (e.g., Kafka).
 * Implementations can add cross-cutting concerns via decorator pattern
 * (retry, throttling, metrics, etc.).
 */
public interface TelemetryPublisher {
    
    /**
     * Publish a message to the specified topic.
     *
     * @param topic the destination topic
     * @param key the message key (for partitioning)
     * @param value the message payload
     */
    void publish(String topic, String key, Object value);
}
