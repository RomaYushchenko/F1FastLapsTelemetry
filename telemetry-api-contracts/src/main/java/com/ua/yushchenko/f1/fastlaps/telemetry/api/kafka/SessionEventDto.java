package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session event payload (topic telemetry.session).
 * See: kafka_contracts_f_1_telemetry.md § 5.1.
 * Display strings: resolve via {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType#fromCode(Integer)}
 * and {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1Track#fromId(Integer)} when needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEventDto {

    private EventCode eventCode;
    /** F1 game session type id (0–18 for F1 25). Used to persist Session.sessionType. */
    private Integer sessionTypeId;
    private Integer trackId;
    private Integer totalLaps;
}
