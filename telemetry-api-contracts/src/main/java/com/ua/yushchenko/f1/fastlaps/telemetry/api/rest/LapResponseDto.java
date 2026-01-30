package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for lap (GET /api/sessions/{sessionUid}/laps).
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 3.2.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapResponseDto {

    private int lapNumber;
    private Integer lapTimeMs;
    private Integer sector1Ms;
    private Integer sector2Ms;
    private Integer sector3Ms;
    private boolean isInvalid;
}
