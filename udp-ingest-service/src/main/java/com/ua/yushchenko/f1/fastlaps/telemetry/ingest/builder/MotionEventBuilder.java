package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link MotionEvent} from packet header and motion payload.
 */
public final class MotionEventBuilder {

    private MotionEventBuilder() {
    }

    /**
     * Build motion event with schema version 1 and producedAt set to now (player car).
     */
    public static MotionEvent build(PacketHeader header, MotionDto payload) {
        return build(header, payload, header.getPlayerCarIndex());
    }

    /**
     * Build motion event for a specific car index (B9: all 22 cars).
     */
    public static MotionEvent build(PacketHeader header, MotionDto payload, int carIndex) {
        return MotionEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.MOTION)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(carIndex)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
