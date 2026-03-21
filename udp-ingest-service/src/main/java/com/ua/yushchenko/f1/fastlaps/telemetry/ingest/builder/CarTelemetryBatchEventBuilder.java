package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetrySlotDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;
import java.util.List;

/**
 * Builds {@link CarTelemetryBatchEvent} for all cars in one Car Telemetry UDP packet.
 */
public final class CarTelemetryBatchEventBuilder {

    private CarTelemetryBatchEventBuilder() {
    }

    public static CarTelemetryBatchEvent build(PacketHeader header, List<CarTelemetrySlotDto> samples) {
        return CarTelemetryBatchEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_TELEMETRY)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .samples(samples)
                .build();
    }
}
