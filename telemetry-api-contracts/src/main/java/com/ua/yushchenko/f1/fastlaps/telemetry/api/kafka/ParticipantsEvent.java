package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for participants (topic telemetry.participants). Includes envelope metadata and a {@link ParticipantsDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ParticipantsEvent extends AbstractTelemetryEvent {

    private ParticipantsDto payload;

    @Override
    public ParticipantsDto getPayload() {
        return payload;
    }
}
