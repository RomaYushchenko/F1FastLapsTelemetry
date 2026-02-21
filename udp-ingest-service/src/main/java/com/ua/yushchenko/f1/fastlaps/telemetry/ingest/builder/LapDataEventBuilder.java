package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link LapDataEvent} from packet header and lap payload.
 */
public final class LapDataEventBuilder {

    private LapDataEventBuilder() {
    }

    /**
     * Build lap data event with schema version 1 and producedAt set to now.
     */
    public static LapDataEvent build(PacketHeader header, LapDto payload) {
        return LapDataEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.LAP_DATA)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
