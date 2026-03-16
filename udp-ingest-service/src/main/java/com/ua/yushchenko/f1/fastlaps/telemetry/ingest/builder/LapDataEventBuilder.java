package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link LapDataEvent} from packet header and lap payload.
 * Use {@link #build(PacketHeader, LapDto, int)} when publishing lap data for all cars (carIndex = which car this lap is for).
 */
public final class LapDataEventBuilder {

    private LapDataEventBuilder() {
    }

    /**
     * Build lap data event with schema version 1 and producedAt set to now.
     * Use when publishing a single lap (e.g. player car only); carIndex = header.getPlayerCarIndex().
     */
    public static LapDataEvent build(PacketHeader header, LapDto payload) {
        return build(header, payload, header.getPlayerCarIndex());
    }

    /**
     * Build lap data event for a specific car index. Use when publishing lap data for all 22 cars.
     * carIndex = which car this lap data is for; playerCarIndex is taken from header.
     */
    public static LapDataEvent build(PacketHeader header, LapDto payload, int carIndex) {
        return LapDataEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.LAP_DATA)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(carIndex)
                .playerCarIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
