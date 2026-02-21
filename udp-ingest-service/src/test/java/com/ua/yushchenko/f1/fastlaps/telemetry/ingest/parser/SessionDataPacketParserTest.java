package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionDataPacketParser")
class SessionDataPacketParserTest {

    private final SessionDataPacketParser parser = new SessionDataPacketParser();

    @Test
    @DisplayName("parse читає 724 байти і повертає DTO з ключовими полями")
    void parseReads724BytesAndReturnsDtoWithKeyFields() {
        // Arrange: minimal 724-byte buffer (F1 25 PacketSessionData payload), key fields set
        ByteBuffer buffer = ByteBuffer.allocate(SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 1);                    // weather (light cloud)
        buffer.put((byte) 45);                   // trackTemperature
        buffer.put((byte) 28);                   // airTemperature
        buffer.put((byte) 57);                   // totalLaps
        buffer.putShort((short) 5000);           // trackLength
        buffer.put((byte) 10);                   // sessionType (RACE)
        buffer.put((byte) 12);                   // trackId
        buffer.put((byte) 0);                    // formula (F1 Modern)
        buffer.putShort((short) 3600);           // sessionTimeLeft
        buffer.putShort((short) 3600);           // sessionDuration
        // remainder zeros (marshal zones, weather forecast, assists, etc.)
        while (buffer.remaining() > 0) {
            buffer.put((byte) 0);
        }
        buffer.flip();

        // Act
        SessionDataDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getWeather()).isEqualTo(1);
        assertThat(dto.getTrackTemperature()).isEqualTo(45);
        assertThat(dto.getAirTemperature()).isEqualTo(28);
        assertThat(dto.getTotalLaps()).isEqualTo(57);
        assertThat(dto.getTrackLength()).isEqualTo(5000);
        assertThat(dto.getSessionType()).isEqualTo(10);
        assertThat(dto.getTrackId()).isEqualTo(12);
        assertThat(dto.getFormula()).isEqualTo(0);
        assertThat(dto.getSessionTimeLeft()).isEqualTo(3600);
        assertThat(dto.getSessionDuration()).isEqualTo(3600);
        assertThat(dto.getMarshalZoneStarts()).hasSize(21);
        assertThat(dto.getWeatherForecastSessionType()).hasSize(64);
        assertThat(dto.getWeekendStructure()).hasSize(12);
        assertThat(buffer.position()).isEqualTo(SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE);
    }

    @Test
    @DisplayName("parse зсуває position на 724 байти")
    void parseAdvancesBufferBy724Bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(730).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);
        buffer.limit(730);

        parser.parse(buffer);

        assertThat(buffer.position()).isEqualTo(SessionDataPacketParser.SESSION_DATA_PAYLOAD_SIZE);
        assertThat(buffer.remaining()).isEqualTo(6);
    }
}
