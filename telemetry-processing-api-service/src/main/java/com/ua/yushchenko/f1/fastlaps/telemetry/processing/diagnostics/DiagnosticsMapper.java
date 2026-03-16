package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import org.springframework.stereotype.Component;

/**
 * Maps raw diagnostic values to REST DTOs.
 */
@Component
public class DiagnosticsMapper {

    public SessionDiagnosticsDto toSessionDiagnosticsDto(
            String sessionPublicId,
            Double packetLossRatio,
            Integer packetHealthPercent,
            String packetHealthBand
    ) {
        return SessionDiagnosticsDto.builder()
                .sessionPublicId(sessionPublicId)
                .packetLossRatio(packetLossRatio)
                .packetHealthPercent(packetHealthPercent)
                .packetHealthBand(packetHealthBand)
                .build();
    }
}

