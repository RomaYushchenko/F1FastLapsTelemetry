package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link EventEvent} from packet header and event payload.
 */
public final class EventEventBuilder {

    private EventEventBuilder() {
    }

    /**
     * Build event with schema version 1 and producedAt set to now.
     * Event is session-wide; carIndex is set to 0.
     */
    public static EventEvent build(PacketHeader header, EventDto payload) {
        return EventEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.EVENT)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex((short) 0)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
