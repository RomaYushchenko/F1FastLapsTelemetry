package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link CarDamageEvent} from packet header and damage payload.
 */
public final class CarDamageEventBuilder {

    private CarDamageEventBuilder() {
    }

    /**
     * Build car damage event with schema version 1 and producedAt set to now.
     */
    public static CarDamageEvent build(PacketHeader header, CarDamageDto payload) {
        return CarDamageEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_DAMAGE)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
