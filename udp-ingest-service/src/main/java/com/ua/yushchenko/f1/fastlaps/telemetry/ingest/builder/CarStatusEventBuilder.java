package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link CarStatusEvent} from packet header and status payload.
 */
public final class CarStatusEventBuilder {

    private CarStatusEventBuilder() {
    }

    /**
     * Build car status event with schema version 1 and producedAt set to now.
     */
    public static CarStatusEvent build(PacketHeader header, CarStatusDto payload) {
        return CarStatusEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_STATUS)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
