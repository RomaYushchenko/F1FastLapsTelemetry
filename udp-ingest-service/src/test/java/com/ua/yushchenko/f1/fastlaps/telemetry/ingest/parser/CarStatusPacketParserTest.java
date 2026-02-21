package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CarStatusPacketParser")
class CarStatusPacketParserTest {

    private final CarStatusPacketParser parser = new CarStatusPacketParser();

    @Test
    @DisplayName("parse читає всі 55 байт CarStatusData і повертає DTO з усіма полями")
    void parseReadsFull55BytesAndReturnsDtoWithAllFields() {
        // Arrange: 55 bytes matching F1 25 CarStatusData layout (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(CarStatusPacketParser.CAR_STATUS_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 2);                    // tractionControl
        buffer.put((byte) 1);                    // antiLockBrakes
        buffer.put((byte) 1);                    // fuelMix
        buffer.put((byte) 55);                   // frontBrakeBias
        buffer.put((byte) 0);                    // pitLimiterStatus
        buffer.putFloat(45.5f);                  // fuelInTank
        buffer.putFloat(110.0f);                 // fuelCapacity
        buffer.putFloat(15.2f);                  // fuelRemainingLaps
        buffer.putShort((short) 12000);          // maxRPM
        buffer.putShort((short) 7000);           // idleRPM
        buffer.put((byte) 8);                    // maxGears
        buffer.put((byte) 1);                    // drsAllowed
        buffer.putShort((short) 300);            // drsActivationDistance
        buffer.put((byte) 18);                   // actualTyreCompound
        buffer.put((byte) 18);                   // visualTyreCompound
        buffer.put((byte) 5);                    // tyresAgeLaps
        buffer.put((byte) 0);                    // vehicleFIAFlags
        buffer.putFloat(750000.0f);               // enginePowerICE
        buffer.putFloat(120000.0f);              // enginePowerMGUK
        buffer.putFloat(2500000.0f);              // ersStoreEnergy
        buffer.put((byte) 2);                    // ersDeployMode
        buffer.putFloat(50000.0f);               // ersHarvestedThisLapMGUK
        buffer.putFloat(30000.0f);               // ersHarvestedThisLapMGUH
        buffer.putFloat(80000.0f);               // ersDeployedThisLap
        buffer.put((byte) 0);                    // networkPaused
        buffer.flip();

        // Act
        CarStatusDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getTractionControl()).isEqualTo(2);
        assertThat(dto.getAbs()).isEqualTo(1);
        assertThat(dto.getFuelMix()).isEqualTo(1);
        assertThat(dto.getFrontBrakeBias()).isEqualTo(55);
        assertThat(dto.getPitLimiterStatus()).isEqualTo(0);
        assertThat(dto.getFuelInTank()).isEqualTo(45.5f);
        assertThat(dto.getFuelCapacity()).isEqualTo(110.0f);
        assertThat(dto.getFuelRemainingLaps()).isEqualTo(15.2f);
        assertThat(dto.getMaxRpm()).isEqualTo(12000);
        assertThat(dto.getIdleRpm()).isEqualTo(7000);
        assertThat(dto.getMaxGears()).isEqualTo(8);
        assertThat(dto.getDrsAllowed()).isTrue();
        assertThat(dto.getDrsActivationDistance()).isEqualTo(300);
        assertThat(dto.getTyresCompound()).isEqualTo(18);
        assertThat(dto.getVisualTyreCompound()).isEqualTo(18);
        assertThat(dto.getTyresAgeLaps()).isEqualTo(5);
        assertThat(dto.getVehicleFiaFlags()).isEqualTo(0);
        assertThat(dto.getEnginePowerIce()).isEqualTo(750000.0f);
        assertThat(dto.getEnginePowerMguk()).isEqualTo(120000.0f);
        assertThat(dto.getErsStoreEnergy()).isEqualTo(2500000.0f);
        assertThat(dto.getErsDeployMode()).isEqualTo(2);
        assertThat(dto.getErsHarvestedThisLapMguk()).isEqualTo(50000.0f);
        assertThat(dto.getErsHarvestedThisLapMguh()).isEqualTo(30000.0f);
        assertThat(dto.getErsDeployedThisLap()).isEqualTo(80000.0f);
        assertThat(dto.getNetworkPaused()).isEqualTo(0);
        assertThat(buffer.remaining()).isZero();
        assertThat(buffer.position()).isEqualTo(CarStatusPacketParser.CAR_STATUS_DATA_SIZE_BYTES);
    }

    @Test
    @DisplayName("parse зсуває position буфера рівно на 55 байт")
    void parseAdvancesBufferBy55Bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(58).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 55; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);
        buffer.limit(58);

        parser.parse(buffer);

        assertThat(buffer.position()).isEqualTo(CarStatusPacketParser.CAR_STATUS_DATA_SIZE_BYTES);
        assertThat(buffer.remaining()).isEqualTo(3);
    }
}
