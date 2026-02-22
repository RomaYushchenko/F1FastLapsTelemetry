package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * Parses F1 2025 Session packet payload (ByteBuffer) into {@link SessionEventDto}.
 * Packet layout: event code (4 bytes), skip 20, session type, track id, total laps.
 * Session type and track display names are resolved from ids in processing (F1SessionType, F1Track).
 */
@Component
public class SessionPacketParser {

    private static final int SKIP_AFTER_EVENT_CODE = 20;

    /**
     * Parse session packet payload into SessionEventDto.
     * Buffer position advances; caller must ensure sufficient remaining bytes.
     * Only raw ids are set; no display strings (single place for mapping is telemetry-api-contracts).
     */
    public SessionEventDto parse(ByteBuffer buffer) {
        byte[] eventCodeBytes = new byte[4];
        buffer.get(eventCodeBytes);
        String eventCodeStr = new String(eventCodeBytes).trim();
        EventCode eventCode = parseEventCode(eventCodeStr);

        buffer.position(buffer.position() + SKIP_AFTER_EVENT_CODE);
        int sessionTypeId = buffer.get() & 0xFF;
        int trackId = buffer.get(); // int8: -1 = unknown
        int totalLaps = buffer.get() & 0xFF;

        return SessionEventDto.builder()
                .eventCode(eventCode)
                .sessionTypeId(sessionTypeId)
                .trackId(trackId)
                .totalLaps(totalLaps)
                .build();
    }

    /**
     * Map F1 4-char event code string to EventCode.
     */
    public static EventCode parseEventCode(String code) {
        return switch (code) {
            case "SSTA" -> EventCode.SSTA;
            case "SEND" -> EventCode.SEND;
            case "FLBK" -> EventCode.FLBK;
            default -> EventCode.SESSION_TIMEOUT;
        };
    }
}
