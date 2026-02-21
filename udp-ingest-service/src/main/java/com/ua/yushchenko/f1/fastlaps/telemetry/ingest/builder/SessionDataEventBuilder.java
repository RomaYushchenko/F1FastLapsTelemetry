package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link SessionDataEvent} from packet header and full session data payload.
 */
public final class SessionDataEventBuilder {

    private SessionDataEventBuilder() {
    }

    /**
     * Build session data event with schema version 1 and producedAt set to now.
     */
    public static SessionDataEvent build(PacketHeader header, SessionDataDto payload) {
        return SessionDataEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.SESSION)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
