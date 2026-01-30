package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session event payload (topic telemetry.session).
 * See: kafka_contracts_f_1_telemetry.md § 5.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEventDto {

    private EventCode eventCode;
    /** e.g. RACE, QUALIFYING */
    private String sessionType;
    private Integer trackId;
    private Integer totalLaps;
}
