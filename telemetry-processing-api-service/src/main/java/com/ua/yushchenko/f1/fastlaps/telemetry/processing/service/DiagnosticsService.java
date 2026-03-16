package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.PacketHealthProperties;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.DiagnosticsMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.PacketLossMetricsReader;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Diagnostics service: resolves session, reads packet loss metric and maps it to health band.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticsService {

    private final SessionResolveService sessionResolveService;
    private final PacketLossMetricsReader packetLossMetricsReader;
    private final DiagnosticsMapper diagnosticsMapper;
    private final PacketHealthProperties packetHealthProperties;

    public SessionDiagnosticsDto getSessionDiagnostics(String sessionPublicId) {
        log.debug("getSessionDiagnostics: sessionPublicId={}", sessionPublicId);

        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionPublicId);
        long sessionUid = session.getSessionUid();

        Optional<Double> lossRatioOpt = packetLossMetricsReader.getPacketLossRatioBySessionUid(sessionUid);
        Double lossRatio = lossRatioOpt.orElse(null);

        Integer healthPercent = null;
        String band;
        if (lossRatio == null) {
            band = "UNKNOWN";
        } else {
            double percent = (1.0d - lossRatio) * 100.0d;
            int roundedPercent = (int) Math.round(percent);
            healthPercent = Math.max(0, Math.min(100, roundedPercent));
            band = resolveBand(healthPercent);
        }

        String publicIdString = SessionMapper.toPublicIdString(session);

        SessionDiagnosticsDto dto = diagnosticsMapper.toSessionDiagnosticsDto(
                publicIdString,
                lossRatio,
                healthPercent,
                band
        );

        log.debug("getSessionDiagnostics: packetHealthBand={}, packetLossRatio={}, packetHealthPercent={}",
                band, lossRatio, healthPercent);

        return dto;
    }

    private String resolveBand(int packetHealthPercent) {
        int goodMin = packetHealthProperties.getGoodMinPercent();
        int okMin = packetHealthProperties.getOkMinPercent();

        if (packetHealthPercent >= goodMin) {
            return "GOOD";
        }
        if (packetHealthPercent >= okMin) {
            return "OK";
        }
        return "POOR";
    }
}

