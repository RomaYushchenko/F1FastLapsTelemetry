package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link ParticipantsEvent} from packet header and participants payload.
 */
public final class ParticipantsEventBuilder {

    private ParticipantsEventBuilder() {
    }

    /**
     * Build participants event with schema version 1 and producedAt set to now.
     * carIndex in envelope set to 0 (participants are per session, not per car).
     */
    public static ParticipantsEvent build(PacketHeader header, ParticipantsDto payload) {
        return ParticipantsEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.PARTICIPANTS)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(0)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
