package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CarTelemetryPacketParser")
class CarTelemetryPacketParserTest {

    private final CarTelemetryPacketParser parser = new CarTelemetryPacketParser();

    @Test
    @DisplayName("parse читає всі 60 байт CarTelemetryData і повертає DTO з усіма полями")
    void parseReadsFull60BytesAndReturnsDtoWithAllFields() {
        // Arrange: 60 bytes matching F1 25 CarTelemetryData layout (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(CarTelemetryPacketParser.CAR_TELEMETRY_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 285);           // speed
        buffer.putFloat(1.0f);                  // throttle
        buffer.putFloat(-0.05f);                // steer
        buffer.putFloat(0.0f);                  // brake
        buffer.put((byte) 0);                   // clutch
        buffer.put((byte) 7);                   // gear
        buffer.putShort((short) 11500);         // engineRPM
        buffer.put((byte) 1);                   // drs
        buffer.put((byte) 75);                  // revLightsPercent
        buffer.putShort((short) 0x3FFF);        // revLightsBitValue
        buffer.putShort((short) 420);           // brakesTemperature[0] RL
        buffer.putShort((short) 418);           // brakesTemperature[1] RR
        buffer.putShort((short) 380);           // brakesTemperature[2] FL
        buffer.putShort((short) 382);           // brakesTemperature[3] FR
        buffer.put((byte) 105);                 // tyresSurfaceTemperature[0-3]
        buffer.put((byte) 106);
        buffer.put((byte) 102);
        buffer.put((byte) 104);
        buffer.put((byte) 108);                 // tyresInnerTemperature[0-3]
        buffer.put((byte) 109);
        buffer.put((byte) 105);
        buffer.put((byte) 107);
        buffer.putShort((short) 95);            // engineTemperature
        buffer.putFloat(23.1f);                 // tyresPressure[0-3]
        buffer.putFloat(23.0f);
        buffer.putFloat(22.9f);
        buffer.putFloat(23.2f);
        buffer.put((byte) 0);                   // surfaceType[0-3]
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 0);
        buffer.flip();

        // Act
        CarTelemetryDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getSpeedKph()).isEqualTo(285);
        assertThat(dto.getThrottle()).isEqualTo(1.0f);
        assertThat(dto.getSteer()).isEqualTo(-0.05f);
        assertThat(dto.getBrake()).isEqualTo(0.0f);
        assertThat(dto.getClutch()).isEqualTo(0);
        assertThat(dto.getGear()).isEqualTo(7);
        assertThat(dto.getEngineRpm()).isEqualTo(11500);
        assertThat(dto.getDrs()).isEqualTo(1);
        assertThat(dto.getRevLightsPercent()).isEqualTo(75);
        assertThat(dto.getRevLightsBitValue()).isEqualTo(0x3FFF);
        assertThat(dto.getBrakesTemperature()).containsExactly(420, 418, 380, 382);
        assertThat(dto.getTyresSurfaceTemperature()).containsExactly(105, 106, 102, 104);
        assertThat(dto.getTyresInnerTemperature()).containsExactly(108, 109, 105, 107);
        assertThat(dto.getEngineTemperature()).isEqualTo(95);
        assertThat(dto.getTyresPressure()).containsExactly(23.1f, 23.0f, 22.9f, 23.2f);
        assertThat(dto.getSurfaceType()).containsExactly(0, 0, 1, 0);
        assertThat(buffer.remaining()).isZero();
        assertThat(buffer.position()).isEqualTo(CarTelemetryPacketParser.CAR_TELEMETRY_DATA_SIZE_BYTES);
    }

    @Test
    @DisplayName("parse зсуває position буфера рівно на 60 байт")
    void parseAdvancesBufferBy60Bytes() {
        // Arrange: buffer with 60 bytes data + 5 extra to assert we only consume 60
        ByteBuffer buffer = ByteBuffer.allocate(65).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 60; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);
        buffer.limit(65);
        int startPos = buffer.position();

        // Act
        parser.parse(buffer);

        // Assert
        assertThat(buffer.position()).isEqualTo(startPos + CarTelemetryPacketParser.CAR_TELEMETRY_DATA_SIZE_BYTES);
        assertThat(buffer.remaining()).isEqualTo(5);
    }
}
