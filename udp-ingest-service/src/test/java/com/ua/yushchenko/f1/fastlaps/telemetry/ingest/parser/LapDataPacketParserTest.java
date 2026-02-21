package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LapDataPacketParser")
class LapDataPacketParserTest {

    private final LapDataPacketParser parser = new LapDataPacketParser();

    @Test
    @DisplayName("parse читає всі 57 байт LapData і повертає DTO з усіма полями")
    void parseReadsFull57BytesAndReturnsDtoWithAllFields() {
        // Arrange: 57 bytes matching F1 25 LapData layout (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(LapDataPacketParser.LAP_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(84500);                   // lastLapTimeInMS
        buffer.putInt(85500);                   // currentLapTimeInMS
        buffer.putShort((short) 28500);         // sector1TimeMSPart
        buffer.put((byte) 0);                    // sector1TimeMinutesPart
        buffer.putShort((short) 30000);         // sector2TimeMSPart
        buffer.put((byte) 0);                    // sector2TimeMinutesPart
        buffer.putShort((short) 1500);          // deltaToCarInFrontMSPart
        buffer.put((byte) 0);                    // deltaToCarInFrontMinutesPart
        buffer.putShort((short) 5000);          // deltaToRaceLeaderMSPart
        buffer.put((byte) 0);                    // deltaToRaceLeaderMinutesPart
        buffer.putFloat(2500.5f);                // lapDistance
        buffer.putFloat(15000.0f);               // totalDistance
        buffer.putFloat(0.5f);                   // safetyCarDelta
        buffer.put((byte) 3);                    // carPosition
        buffer.put((byte) 5);                    // currentLapNum
        buffer.put((byte) 0);                    // pitStatus
        buffer.put((byte) 1);                   // numPitStops
        buffer.put((byte) 2);                    // sector
        buffer.put((byte) 0);                    // currentLapInvalid
        buffer.put((byte) 0);                   // penalties
        buffer.put((byte) 1);                   // totalWarnings
        buffer.put((byte) 0);                   // cornerCuttingWarnings
        buffer.put((byte) 0);                   // numUnservedDriveThroughPens
        buffer.put((byte) 0);                   // numUnservedStopGoPens
        buffer.put((byte) 2);                    // gridPosition
        buffer.put((byte) 4);                   // driverStatus (on track)
        buffer.put((byte) 2);                    // resultStatus (active)
        buffer.put((byte) 0);                    // pitLaneTimerActive
        buffer.putShort((short) 0);             // pitLaneTimeInLaneInMS
        buffer.putShort((short) 25000);         // pitStopTimerInMS
        buffer.put((byte) 0);                    // pitStopShouldServePen
        buffer.putFloat(312.5f);                 // speedTrapFastestSpeed
        buffer.put((byte) 4);                    // speedTrapFastestLap
        buffer.flip();

        // Act
        LapDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getLastLapTimeMs()).isEqualTo(84500);
        assertThat(dto.getCurrentLapTimeMs()).isEqualTo(85500);
        assertThat(dto.getSector1TimeMs()).isEqualTo(28500);
        assertThat(dto.getSector2TimeMs()).isEqualTo(30000);
        assertThat(dto.getDeltaToCarInFrontMs()).isEqualTo(1500);
        assertThat(dto.getDeltaToRaceLeaderMs()).isEqualTo(5000);
        assertThat(dto.getLapDistance()).isEqualTo(2500.5f);
        assertThat(dto.getTotalDistance()).isEqualTo(15000.0f);
        assertThat(dto.getSafetyCarDelta()).isEqualTo(0.5f);
        assertThat(dto.getCarPosition()).isEqualTo(3);
        assertThat(dto.getLapNumber()).isEqualTo(5);
        assertThat(dto.getPitStatus()).isEqualTo(0);
        assertThat(dto.getNumPitStops()).isEqualTo(1);
        assertThat(dto.getSector()).isEqualTo(2);
        assertThat(dto.isInvalid()).isFalse();
        assertThat(dto.getTotalWarnings()).isEqualTo(1);
        assertThat(dto.getGridPosition()).isEqualTo(2);
        assertThat(dto.getDriverStatus()).isEqualTo(4);
        assertThat(dto.getResultStatus()).isEqualTo(2);
        assertThat(dto.getPitStopTimerInMs()).isEqualTo(25000);
        assertThat(dto.getSpeedTrapFastestSpeed()).isEqualTo(312.5f);
        assertThat(dto.getSpeedTrapFastestLap()).isEqualTo(4);
        assertThat(buffer.remaining()).isZero();
        assertThat(buffer.position()).isEqualTo(LapDataPacketParser.LAP_DATA_SIZE_BYTES);
    }

    @Test
    @DisplayName("parse зсуває position буфера рівно на 57 байт")
    void parseAdvancesBufferBy57Bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(60).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 57; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);
        buffer.limit(60);

        parser.parse(buffer);

        assertThat(buffer.position()).isEqualTo(LapDataPacketParser.LAP_DATA_SIZE_BYTES);
        assertThat(buffer.remaining()).isEqualTo(3);
    }
}
