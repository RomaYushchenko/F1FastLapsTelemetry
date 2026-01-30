package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for session summary (GET /api/sessions/{sessionUid}/summary).
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 3.3.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummaryDto {

    private Integer totalLaps;
    private Integer bestLapTimeMs;
    private Integer bestLapNumber;
    private Integer bestSector1Ms;
    private Integer bestSector2Ms;
    private Integer bestSector3Ms;
}
