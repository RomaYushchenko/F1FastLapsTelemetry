package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for Packet Event (topic telemetry.event). Includes envelope metadata and {@link EventDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EventEvent extends AbstractTelemetryEvent {

    private EventDto payload;

    @Override
    public EventDto getPayload() {
        return payload;
    }
}
