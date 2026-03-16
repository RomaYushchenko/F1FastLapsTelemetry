package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.GlobalDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.PacketHealthProperties;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.PacketLossMetricsReader;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds global diagnostics snapshot. For now focuses on aggregated packet health.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalDiagnosticsService {

    private final SessionRepository sessionRepository;
    private final PacketLossMetricsReader packetLossMetricsReader;
    private final PacketHealthProperties packetHealthProperties;

    public GlobalDiagnosticsDto getGlobalDiagnostics() {
        log.debug("getGlobalDiagnostics: building global diagnostics snapshot");

        // For a first version, aggregate packet loss across all sessions that have metrics.
        List<Session> sessions = sessionRepository.findAll();

        double sumLoss = 0.0d;
        int count = 0;
        for (Session session : sessions) {
            Long sessionUid = session.getSessionUid();
            if (sessionUid == null) {
                continue;
            }
            var lossOpt = packetLossMetricsReader.getPacketLossRatioBySessionUid(sessionUid);
            if (lossOpt.isPresent()) {
                sumLoss += lossOpt.get();
                count++;
            }
        }

        Double lossRatio = null;
        Integer healthPercent = null;
        String band = "UNKNOWN";

        if (count > 0) {
            double avgLoss = sumLoss / count;
            lossRatio = Math.max(0.0d, Math.min(1.0d, avgLoss));
            double percent = (1.0d - lossRatio) * 100.0d;
            int roundedPercent = (int) Math.round(percent);
            healthPercent = Math.max(0, Math.min(100, roundedPercent));

            int goodMin = packetHealthProperties.getGoodMinPercent();
            int okMin = packetHealthProperties.getOkMinPercent();
            if (healthPercent >= goodMin) {
                band = "GOOD";
            } else if (healthPercent >= okMin) {
                band = "OK";
            } else {
                band = "POOR";
            }
        }

        return GlobalDiagnosticsDto.builder()
                .statusMessage("OK")
                .packetLossRatio(lossRatio)
                .packetHealthBand(band)
                .packetHealthPercent(healthPercent)
                .build();
    }
}

