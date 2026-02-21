package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for full session data (topic telemetry.sessionData). Includes envelope metadata and a {@link SessionDataDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionDataEvent extends AbstractTelemetryEvent {

    private SessionDataDto payload;

    @Override
    public SessionDataDto getPayload() {
        return payload;
    }
}
