package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for lap data (topic telemetry.lap). Includes envelope metadata and a {@link LapDto} payload.
 * When ingest publishes lap data for all 22 cars, carIndex is the car this lap data is for;
 * playerCarIndex is from the packet header (which car is the human player).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LapDataEvent extends AbstractTelemetryEvent {

    private LapDto payload;
    /** Player car index from packet header (m_playerCarIndex). Used by consumer to set session player car. */
    private int playerCarIndex;

    @Override
    public LapDto getPayload() {
        return payload;
    }
}
