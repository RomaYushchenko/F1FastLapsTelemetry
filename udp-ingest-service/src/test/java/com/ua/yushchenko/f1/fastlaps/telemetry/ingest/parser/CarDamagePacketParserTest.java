package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CarDamagePacketParser")
class CarDamagePacketParserTest {

    private final CarDamagePacketParser parser = new CarDamagePacketParser();

    @Test
    @DisplayName("parse читає всі 46 байт CarDamageData і повертає DTO з усіма полями")
    void parseReadsFull46BytesAndReturnsDtoWithAllFields() {
        // Arrange: 46 bytes matching F1 25 CarDamageData layout (little-endian)
        ByteBuffer buffer = ByteBuffer.allocate(CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(0.12f);   // tyresWear RL, RR, FL, FR
        buffer.putFloat(0.15f);
        buffer.putFloat(0.08f);
        buffer.putFloat(0.10f);
        buffer.put((byte) 2);    // tyresDamage[4]
        buffer.put((byte) 3);
        buffer.put((byte) 5);
        buffer.put((byte) 1);
        buffer.put((byte) 0);    // brakesDamage[4]
        buffer.put((byte) 0);
        buffer.put((byte) 10);
        buffer.put((byte) 0);
        buffer.put((byte) 0);    // tyreBlisters[4]
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 5);    // frontLeftWingDamage
        buffer.put((byte) 0);    // frontRightWingDamage
        buffer.put((byte) 0);    // rearWingDamage
        buffer.put((byte) 0);    // floorDamage
        buffer.put((byte) 0);    // diffuserDamage
        buffer.put((byte) 0);    // sidepodDamage
        buffer.put((byte) 1);    // drsFault
        buffer.put((byte) 0);    // ersFault
        buffer.put((byte) 0);    // gearBoxDamage
        buffer.put((byte) 0);    // engineDamage
        buffer.put((byte) 0);    // engineMGUHWear
        buffer.put((byte) 0);    // engineESWear
        buffer.put((byte) 0);    // engineCEWear
        buffer.put((byte) 0);    // engineICEWear
        buffer.put((byte) 0);    // engineMGUKWear
        buffer.put((byte) 0);    // engineTCWear
        buffer.put((byte) 0);    // engineBlown
        buffer.put((byte) 0);    // engineSeized
        buffer.flip();

        // Act
        CarDamageDto dto = parser.parse(buffer);

        // Assert
        assertThat(dto.getTyresWearRL()).isEqualTo(0.12f);
        assertThat(dto.getTyresWearRR()).isEqualTo(0.15f);
        assertThat(dto.getTyresWearFL()).isEqualTo(0.08f);
        assertThat(dto.getTyresWearFR()).isEqualTo(0.10f);
        assertThat(dto.getTyresDamage()).containsExactly(2, 3, 5, 1);
        assertThat(dto.getBrakesDamage()).containsExactly(0, 0, 10, 0);
        assertThat(dto.getTyreBlisters()).containsExactly(0, 0, 0, 0);
        assertThat(dto.getFrontLeftWingDamage()).isEqualTo(5);
        assertThat(dto.getDrsFault()).isEqualTo(1);
        assertThat(dto.getErsFault()).isEqualTo(0);
        assertThat(buffer.remaining()).isZero();
        assertThat(buffer.position()).isEqualTo(CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES);
    }

    @Test
    @DisplayName("parse зсуває position буфера рівно на 46 байт")
    void parseAdvancesBufferBy46Bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 46; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);
        buffer.limit(50);

        parser.parse(buffer);

        assertThat(buffer.position()).isEqualTo(CarDamagePacketParser.CAR_DAMAGE_DATA_SIZE_BYTES);
        assertThat(buffer.remaining()).isEqualTo(4);
    }
}
