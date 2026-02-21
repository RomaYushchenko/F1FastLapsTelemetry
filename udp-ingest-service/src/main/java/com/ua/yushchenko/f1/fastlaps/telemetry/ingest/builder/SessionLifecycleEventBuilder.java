package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link SessionLifecycleEvent} from packet header and session payload.
 */
public final class SessionLifecycleEventBuilder {

    private SessionLifecycleEventBuilder() {
    }

    /**
     * Build session lifecycle event with schema version 1 and producedAt set to now.
     */
    public static SessionLifecycleEvent build(PacketHeader header, SessionEventDto payload) {
        return SessionLifecycleEvent.builder()
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
