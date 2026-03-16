package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PacketLossMetricsReader implementation backed by Micrometer MeterRegistry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MicrometerPacketLossMetricsReader implements PacketLossMetricsReader {

    private static final String METRIC_NAME = "f1.telemetry.packet_loss_ratio";

    private final MeterRegistry meterRegistry;

    @Override
    public Optional<Double> getPacketLossRatioBySessionUid(long sessionUid) {
        String sessionUidTag = String.valueOf(sessionUid);
        Search search = meterRegistry.find(METRIC_NAME)
                .tag("session_uid", sessionUidTag);

        Double value = search.gauge() != null ? search.gauge().value() : null;
        if (value == null || Double.isNaN(value)) {
            log.debug("packet_loss_ratio metric not available for sessionUid={}", sessionUidTag);
            return Optional.empty();
        }
        double clamped = Math.max(0.0d, Math.min(1.0d, value));
        return Optional.of(clamped);
    }
}


