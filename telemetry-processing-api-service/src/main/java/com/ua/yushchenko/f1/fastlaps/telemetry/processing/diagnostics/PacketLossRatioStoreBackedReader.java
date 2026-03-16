package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Packet loss metrics reader backed by in-memory store populated from Kafka (telemetry.packetHealth).
 * Used as primary source so diagnostics API returns real-time data from udp-ingest-service.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class PacketLossRatioStoreBackedReader implements PacketLossMetricsReader {

    private final PacketLossRatioStore packetLossRatioStore;

    @Override
    public Optional<Double> getPacketLossRatioBySessionUid(long sessionUid) {
        return packetLossRatioStore.get(sessionUid);
    }
}
