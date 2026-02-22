package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 Packet Event payload (45 bytes: 4-byte event code + 41-byte union EventDataDetails)
 * into {@link EventDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (Event – 45 bytes).
 */
@Component
public class EventPacketParser {

    /** Packet Event payload size after header (4 event code + 41 union). */
    public static final int EVENT_PAYLOAD_SIZE = 45;
    private static final int EVENT_CODE_LEN = 4;

    /**
     * Parse event payload from current buffer position. Buffer must have at least 45 bytes remaining.
     * Advances buffer position by 45 bytes.
     */
    public EventDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();

        byte[] codeBytes = new byte[EVENT_CODE_LEN];
        buffer.get(codeBytes);
        String eventCode = new String(codeBytes).trim();

        EventDto.EventDtoBuilder builder = EventDto.builder().eventCode(eventCode);

        switch (eventCode) {
            case "FTLP" -> parseFastestLap(buffer, builder);
            case "RTMT" -> parseRetirement(buffer, builder);
            case "DRSD" -> parseDrsDisabled(buffer, builder);
            case "DRSE" -> { /* no details */ }
            case "TMPT" -> parseTeamMateInPits(buffer, builder);
            case "RCWN" -> parseRaceWinner(buffer, builder);
            case "PENA" -> parsePenalty(buffer, builder);
            case "SPTP" -> parseSpeedTrap(buffer, builder);
            case "STLG" -> parseStartLights(buffer, builder);
            case "DTSV" -> parseDriveThroughServed(buffer, builder);
            case "SGSV" -> parseStopGoServed(buffer, builder);
            case "FLBK" -> parseFlashback(buffer, builder);
            case "OVTK" -> parseOvertake(buffer, builder);
            case "SCAR" -> parseSafetyCar(buffer, builder);
            case "COLL" -> parseCollision(buffer, builder);
            default -> { /* SSTA, SEND, CHQF, LGOT, BUTN, RDFL etc. – no details or skip */ }
        }

        buffer.position(startPos + EVENT_PAYLOAD_SIZE);
        return builder.build();
    }

    private void parseFastestLap(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int vehicleIdx = buffer.get() & 0xFF;
        float lapTime = buffer.getFloat();
        builder.vehicleIdx(vehicleIdx).lapTime(lapTime);
    }

    private void parseRetirement(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int vehicleIdx = buffer.get() & 0xFF;
        int reason = buffer.get() & 0xFF;
        builder.vehicleIdx(vehicleIdx).retirementReason(reason);
    }

    private void parseDrsDisabled(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int reason = buffer.get() & 0xFF;
        builder.drsDisabledReason(reason);
    }

    private void parseTeamMateInPits(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        builder.vehicleIdx(buffer.get() & 0xFF);
    }

    private void parseRaceWinner(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        builder.vehicleIdx(buffer.get() & 0xFF);
    }

    private void parsePenalty(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int penaltyType = buffer.get() & 0xFF;
        int infringementType = buffer.get() & 0xFF;
        int vehicleIdx = buffer.get() & 0xFF;
        int otherVehicleIdx = buffer.get() & 0xFF;
        int time = buffer.get() & 0xFF;
        int lapNum = buffer.get() & 0xFF;
        int placesGained = buffer.get() & 0xFF;
        builder.penaltyType(penaltyType).infringementType(infringementType)
                .vehicleIdx(vehicleIdx).otherVehicleIdx(otherVehicleIdx)
                .penaltyTime(time).penaltyLapNum(lapNum).placesGained(placesGained);
    }

    private void parseSpeedTrap(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int vehicleIdx = buffer.get() & 0xFF;
        float speed = buffer.getFloat();
        int isOverallFastest = buffer.get() & 0xFF;
        int isDriverFastest = buffer.get() & 0xFF;
        int fastestVehicleIdx = buffer.get() & 0xFF;
        float fastestSpeed = buffer.getFloat();
        builder.vehicleIdx(vehicleIdx).speedTrapSpeedKph(speed)
                .isOverallFastestInSession(isOverallFastest).isDriverFastestInSession(isDriverFastest)
                .fastestVehicleIdxInSession(fastestVehicleIdx).fastestSpeedInSession(fastestSpeed);
    }

    private void parseStartLights(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        builder.numLights(buffer.get() & 0xFF);
    }

    private void parseDriveThroughServed(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        builder.vehicleIdx(buffer.get() & 0xFF);
    }

    private void parseStopGoServed(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int vehicleIdx = buffer.get() & 0xFF;
        float stopTime = buffer.getFloat();
        builder.vehicleIdx(vehicleIdx).stopTimeSeconds(stopTime);
    }

    private void parseFlashback(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        long frameId = buffer.getInt() & 0xFFFFFFFFL;
        float sessionTime = buffer.getFloat();
        builder.flashbackFrameIdentifier(frameId).flashbackSessionTime(sessionTime);
    }

    private void parseOvertake(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int overtaking = buffer.get() & 0xFF;
        int beingOvertaken = buffer.get() & 0xFF;
        builder.overtakingVehicleIdx(overtaking).beingOvertakenVehicleIdx(beingOvertaken);
    }

    private void parseSafetyCar(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int safetyCarType = buffer.get() & 0xFF;
        int eventType = buffer.get() & 0xFF;
        builder.safetyCarType(safetyCarType).safetyCarEventType(eventType);
    }

    private void parseCollision(ByteBuffer buffer, EventDto.EventDtoBuilder builder) {
        int v1 = buffer.get() & 0xFF;
        int v2 = buffer.get() & 0xFF;
        builder.vehicle1Idx(v1).vehicle2Idx(v2);
    }
}
