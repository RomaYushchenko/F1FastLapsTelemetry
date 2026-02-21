package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 LapData from ByteBuffer into {@link LapDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (57 bytes per car).
 */
@Component
public class LapDataPacketParser {

    /** Size in bytes of one LapData struct (F1 25 spec). */
    public static final int LAP_DATA_SIZE_BYTES = 57;

    /**
     * Parse one car's lap data from current buffer position. Buffer must be positioned at start of LapData.
     * Advances buffer position by {@value #LAP_DATA_SIZE_BYTES} bytes.
     */
    public LapDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int lastLapTimeMs = buffer.getInt();
        int currentLapTimeMs = buffer.getInt();
        int sector1MsPart = buffer.getShort() & 0xFFFF;
        int sector1MinPart = buffer.get() & 0xFF;
        int sector2MsPart = buffer.getShort() & 0xFFFF;
        int sector2MinPart = buffer.get() & 0xFF;
        int sector1TimeMs = sector1MinPart * 60_000 + sector1MsPart;
        int sector2TimeMs = sector2MinPart * 60_000 + sector2MsPart;

        int deltaToCarInFrontMsPart = buffer.getShort() & 0xFFFF;
        int deltaToCarInFrontMinPart = buffer.get() & 0xFF;
        int deltaToRaceLeaderMsPart = buffer.getShort() & 0xFFFF;
        int deltaToRaceLeaderMinPart = buffer.get() & 0xFF;
        int deltaToCarInFrontMs = deltaToCarInFrontMinPart * 60_000 + deltaToCarInFrontMsPart;
        int deltaToRaceLeaderMs = deltaToRaceLeaderMinPart * 60_000 + deltaToRaceLeaderMsPart;

        float lapDistance = buffer.getFloat();
        float totalDistance = buffer.getFloat();
        float safetyCarDelta = buffer.getFloat();
        int carPosition = buffer.get() & 0xFF;
        int currentLapNum = buffer.get() & 0xFF;
        int pitStatus = buffer.get() & 0xFF;
        int numPitStops = buffer.get() & 0xFF;
        int sector = buffer.get() & 0xFF;
        int currentLapInvalid = buffer.get() & 0xFF;
        int penalties = buffer.get() & 0xFF;
        int totalWarnings = buffer.get() & 0xFF;
        int cornerCuttingWarnings = buffer.get() & 0xFF;
        int numUnservedDriveThroughPens = buffer.get() & 0xFF;
        int numUnservedStopGoPens = buffer.get() & 0xFF;
        int gridPosition = buffer.get() & 0xFF;
        int driverStatus = buffer.get() & 0xFF;
        int resultStatus = buffer.get() & 0xFF;
        int pitLaneTimerActive = buffer.get() & 0xFF;
        int pitLaneTimeInLaneInMs = buffer.getShort() & 0xFFFF;
        int pitStopTimerInMs = buffer.getShort() & 0xFFFF;
        int pitStopShouldServePen = buffer.get() & 0xFF;
        float speedTrapFastestSpeed = buffer.getFloat();
        int speedTrapFastestLap = buffer.get() & 0xFF;

        return LapDto.builder()
                .lapNumber(currentLapNum)
                .lapDistance(lapDistance)
                .lastLapTimeMs(lastLapTimeMs > 0 ? lastLapTimeMs : null)
                .currentLapTimeMs(currentLapTimeMs > 0 ? currentLapTimeMs : null)
                .sector1TimeMs(sector1TimeMs > 0 ? sector1TimeMs : null)
                .sector2TimeMs(sector2TimeMs > 0 ? sector2TimeMs : null)
                .sector(sector)
                .isInvalid(currentLapInvalid == 1)
                .penaltiesSeconds(penalties > 0 ? penalties : null)
                .deltaToCarInFrontMs(deltaToCarInFrontMs != 0 ? deltaToCarInFrontMs : null)
                .deltaToRaceLeaderMs(deltaToRaceLeaderMs != 0 ? deltaToRaceLeaderMs : null)
                .totalDistance(totalDistance)
                .safetyCarDelta(safetyCarDelta)
                .carPosition(carPosition)
                .pitStatus(pitStatus)
                .numPitStops(numPitStops)
                .totalWarnings(totalWarnings)
                .cornerCuttingWarnings(cornerCuttingWarnings)
                .numUnservedDriveThroughPens(numUnservedDriveThroughPens)
                .numUnservedStopGoPens(numUnservedStopGoPens)
                .gridPosition(gridPosition)
                .driverStatus(driverStatus)
                .resultStatus(resultStatus)
                .pitLaneTimerActive(pitLaneTimerActive)
                .pitLaneTimeInLaneInMs(pitLaneTimeInLaneInMs)
                .pitStopTimerInMs(pitStopTimerInMs)
                .pitStopShouldServePen(pitStopShouldServePen)
                .speedTrapFastestSpeed(speedTrapFastestSpeed)
                .speedTrapFastestLap(speedTrapFastestLap != 255 ? speedTrapFastestLap : null)
                .build();
    }
}
