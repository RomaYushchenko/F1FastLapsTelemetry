package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParticipantsPacketParser")
class ParticipantsPacketParserTest {

    private final ParticipantsPacketParser parser = new ParticipantsPacketParser();

    @Test
    @DisplayName("parse повертає numActiveCars та список учасників з raceNumber і name")
    void parseReturnsNumActiveCarsAndParticipantsWithRaceNumberAndName() {
        // Arrange: numActiveCars=2, then 2 x ParticipantData (56 bytes each), then 20 x empty ParticipantData
        ByteBuffer buffer = ByteBuffer.allocate(1 + ParticipantsPacketParser.NUM_CARS * ParticipantsPacketParser.PARTICIPANT_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 2); // numActiveCars

        // Participant 0: raceNumber=44, name="HAM"
        writeParticipantData(buffer, 0, 44, "HAM");
        // Participant 1: raceNumber=1, name="VER"
        writeParticipantData(buffer, 1, 1, "VER");
        // Remaining 20 participants: zeros
        for (int i = 2; i < ParticipantsPacketParser.NUM_CARS; i++) {
            writeParticipantData(buffer, i, 0, null);
        }
        buffer.flip();

        // Act
        ParticipantsDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getNumActiveCars()).isEqualTo(2);
        assertThat(dto.getParticipants()).hasSize(ParticipantsPacketParser.NUM_CARS);
        ParticipantDataDto p0 = dto.getParticipants().get(0);
        assertThat(p0.getCarIndex()).isEqualTo(0);
        assertThat(p0.getRaceNumber()).isEqualTo(44);
        assertThat(p0.getName()).isEqualTo("HAM");
        ParticipantDataDto p1 = dto.getParticipants().get(1);
        assertThat(p1.getCarIndex()).isEqualTo(1);
        assertThat(p1.getRaceNumber()).isEqualTo(1);
        assertThat(p1.getName()).isEqualTo("VER");
    }

    private void writeParticipantData(ByteBuffer buffer, int carIndex, int raceNumber, String name) {
        buffer.put((byte) 0);   // m_aiControlled
        buffer.put((byte) 1);   // m_driverId
        buffer.put((byte) 0);   // m_networkId
        buffer.put((byte) 0);   // m_teamId
        buffer.put((byte) 0);   // m_myTeam
        buffer.put((byte) raceNumber);
        buffer.put((byte) 0);   // m_nationality
        byte[] nameBytes = new byte[32];
        if (name != null) {
            byte[] src = name.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, nameBytes, 0, Math.min(src.length, 32));
        }
        buffer.put(nameBytes);
        buffer.put((byte) 0);   // m_yourTelemetry
        buffer.put((byte) 0);   // m_showOnlineNames
        buffer.putShort((short) 0); // m_techLevel
        buffer.put((byte) 0);   // m_platform
        buffer.put((byte) 0);   // m_numColours
        buffer.put(new byte[12]); // m_liveryColours[4]
    }
}
