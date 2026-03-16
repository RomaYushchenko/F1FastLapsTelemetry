package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diagnostics DTO for a single session.
 * Exposed via GET /api/sessions/{publicId}/diagnostics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDiagnosticsDto {

    /**
     * Public session identifier (UUID string).
     * Matches SessionDto.id and is used in URLs.
     */
    private String sessionPublicId;

    /**
     * Packet loss ratio in range [0.0, 1.0].
     * Null when metric is not yet available for this session.
     */
    private Double packetLossRatio;

    /**
     * Packet health band: GOOD, OK, POOR or UNKNOWN.
     */
    private String packetHealthBand;

    /**
     * Packet health percent in range [0, 100].
     * Represents (1 - packetLossRatio) * 100, rounded.
     * Null when packetLossRatio is null.
     */
    private Integer packetHealthPercent;
}

