package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses F1 25 PacketParticipantsData payload (after header) into {@link ParticipantsDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt: 1 byte numActiveCars, then 22 x ParticipantData (56 bytes each).
 */
@Component
public class ParticipantsPacketParser {

    /** Max cars in UDP data (F1 25). */
    public static final int NUM_CARS = 22;
    /** Size in bytes of one ParticipantData struct (F1 25 spec): 7+32+6+12=57. */
    public static final int PARTICIPANT_DATA_SIZE_BYTES = 57;
    /** Max participant name length in bytes. */
    private static final int NAME_LENGTH = 32;

    /**
     * Parse full participants payload from current buffer position.
     * Payload: numActiveCars (1), then 22 x ParticipantData (56 bytes each).
     * Advances buffer position by 1 + 22 * 56 bytes.
     */
    public ParticipantsDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int numActiveCars = buffer.get() & 0xFF;
        List<ParticipantDataDto> participants = new ArrayList<>(NUM_CARS);
        for (int carIndex = 0; carIndex < NUM_CARS; carIndex++) {
            ParticipantDataDto dto = parseOneParticipant(buffer, carIndex);
            participants.add(dto);
        }
        return ParticipantsDto.builder()
                .numActiveCars(numActiveCars)
                .participants(participants)
                .build();
    }

    /**
     * Parse one ParticipantData from current buffer position. Advances by {@value #PARTICIPANT_DATA_SIZE_BYTES} bytes.
     */
    public ParticipantDataDto parseOneParticipant(ByteBuffer buffer, int carIndex) {
        buffer.get(); // m_aiControlled
        int driverId = buffer.get() & 0xFF;
        buffer.get(); // m_networkId
        int teamId = buffer.get() & 0xFF;
        buffer.get(); // m_myTeam
        int raceNumber = buffer.get() & 0xFF;
        buffer.get(); // m_nationality
        String name = readParticipantName(buffer);
        buffer.get(); // m_yourTelemetry
        buffer.get(); // m_showOnlineNames
        buffer.getShort(); // m_techLevel
        buffer.get(); // m_platform
        buffer.get(); // m_numColours
        buffer.position(buffer.position() + 12); // m_liveryColours[4] = 4 * 3 bytes

        String nameTrimmed = name != null && !name.isBlank() ? name.trim() : null;
        Integer raceNum = (raceNumber != 0 && raceNumber != 255) ? raceNumber : null;

        return ParticipantDataDto.builder()
                .carIndex(carIndex)
                .raceNumber(raceNum)
                .name(nameTrimmed)
                .driverId(driverId != 255 ? driverId : null)
                .teamId(teamId != 255 ? teamId : null)
                .build();
    }

    private static String readParticipantName(ByteBuffer buffer) {
        byte[] bytes = new byte[NAME_LENGTH];
        buffer.get(bytes);
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
