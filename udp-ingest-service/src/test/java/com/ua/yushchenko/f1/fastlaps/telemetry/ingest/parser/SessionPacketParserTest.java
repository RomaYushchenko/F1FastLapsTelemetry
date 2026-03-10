package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionPacketParser")
class SessionPacketParserTest {

    private final SessionPacketParser parser = new SessionPacketParser();

    /** Short SSTA packet: 4-byte code, skip 20, then sessionType (u8), trackId (i8), totalLaps (u8). */
    @Test
    @DisplayName("parse SSTA: sessionType, trackId, totalLaps order")
    void parseSstaSessionTypeTrackIdTotalLaps() {
        ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("SSTA".getBytes());
        for (int i = 0; i < 20; i++) {
            buffer.put((byte) 0);
        }
        buffer.put((byte) 8);    // sessionType (e.g. Short Qualifying)
        buffer.put((byte) 3);    // trackId (Sakhir/Bahrain)
        buffer.put((byte) 57);   // totalLaps
        buffer.flip();

        SessionEventDto dto = parser.parse(buffer);

        assertThat(dto.getEventCode()).isEqualTo(EventCode.SSTA);
        assertThat(dto.getSessionTypeId()).isEqualTo(8);
        assertThat(dto.getTrackId()).isEqualTo(3);
        assertThat(dto.getTotalLaps()).isEqualTo(57);
    }
}
