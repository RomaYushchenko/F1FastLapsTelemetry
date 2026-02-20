package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for session lifecycle (topic telemetry.session). Includes envelope metadata and a {@link SessionEventDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionLifecycleEvent extends AbstractTelemetryEvent {

    private SessionEventDto payload;

    @Override
    public SessionEventDto getPayload() {
        return payload;
    }
}
