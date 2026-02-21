package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of {@link TelemetryPublisher}.
 * Used when Kafka publishing is disabled. Logs messages at DEBUG level.
 */
@Slf4j
public class NoOpPublisher implements TelemetryPublisher {

    @Override
    public void publish(String topic, String key, Object value) {
        log.debug("NoOp publish (Kafka disabled): topic={}, key={}", topic, key);
    }
}
