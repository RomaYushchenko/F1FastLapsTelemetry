package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Global diagnostics payload.
 * Designed to be extended with more signals over time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalDiagnosticsDto {

    /**
     * Optional message describing the overall system status.
     * For example: "OK", "Degraded", "Maintenance".
     */
    private String statusMessage;

    /**
     * Optional packet loss ratio aggregated across active sessions.
     * When null, packet loss diagnostics may be unavailable globally.
     */
    private Double packetLossRatio;

    /**
     * Packet health band derived from packetLossRatio: GOOD, OK, POOR or UNKNOWN.
     */
    private String packetHealthBand;

    /**
     * Derived packet health percent (0–100) for aggregated packet loss.
     * Null when packetLossRatio is null.
     */
    private Integer packetHealthPercent;
}

