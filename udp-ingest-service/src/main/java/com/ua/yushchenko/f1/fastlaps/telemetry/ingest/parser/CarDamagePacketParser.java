package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 CarDamageData from ByteBuffer into {@link CarDamageDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (46 bytes per car).
 * Order: tyresWear[4], tyresDamage[4], brakesDamage[4], tyreBlisters[4], then 18 single-byte fields.
 */
@Component
public class CarDamagePacketParser {

    /** Size in bytes of one CarDamageData struct (F1 25 spec). */
    public static final int CAR_DAMAGE_DATA_SIZE_BYTES = 46;

    /**
     * Parse one car's damage from current buffer position. Buffer must be positioned at start of CarDamageData.
     * Advances buffer position by {@value #CAR_DAMAGE_DATA_SIZE_BYTES} bytes.
     */
    public CarDamageDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        float tyresWearRL = buffer.getFloat();
        float tyresWearRR = buffer.getFloat();
        float tyresWearFL = buffer.getFloat();
        float tyresWearFR = buffer.getFloat();

        int[] tyresDamage = new int[4];
        for (int i = 0; i < 4; i++) {
            tyresDamage[i] = buffer.get() & 0xFF;
        }
        int[] brakesDamage = new int[4];
        for (int i = 0; i < 4; i++) {
            brakesDamage[i] = buffer.get() & 0xFF;
        }
        int[] tyreBlisters = new int[4];
        for (int i = 0; i < 4; i++) {
            tyreBlisters[i] = buffer.get() & 0xFF;
        }

        int frontLeftWingDamage = buffer.get() & 0xFF;
        int frontRightWingDamage = buffer.get() & 0xFF;
        int rearWingDamage = buffer.get() & 0xFF;
        int floorDamage = buffer.get() & 0xFF;
        int diffuserDamage = buffer.get() & 0xFF;
        int sidepodDamage = buffer.get() & 0xFF;
        int drsFault = buffer.get() & 0xFF;
        int ersFault = buffer.get() & 0xFF;
        int gearBoxDamage = buffer.get() & 0xFF;
        int engineDamage = buffer.get() & 0xFF;
        int engineMguhWear = buffer.get() & 0xFF;
        int engineEsWear = buffer.get() & 0xFF;
        int engineCeWear = buffer.get() & 0xFF;
        int engineIceWear = buffer.get() & 0xFF;
        int engineMgukWear = buffer.get() & 0xFF;
        int engineTcWear = buffer.get() & 0xFF;
        int engineBlown = buffer.get() & 0xFF;
        int engineSeized = buffer.get() & 0xFF;

        return CarDamageDto.builder()
                .tyresWearRL(tyresWearRL)
                .tyresWearRR(tyresWearRR)
                .tyresWearFL(tyresWearFL)
                .tyresWearFR(tyresWearFR)
                .tyresDamage(tyresDamage)
                .brakesDamage(brakesDamage)
                .tyreBlisters(tyreBlisters)
                .frontLeftWingDamage(frontLeftWingDamage)
                .frontRightWingDamage(frontRightWingDamage)
                .rearWingDamage(rearWingDamage)
                .floorDamage(floorDamage)
                .diffuserDamage(diffuserDamage)
                .sidepodDamage(sidepodDamage)
                .drsFault(drsFault)
                .ersFault(ersFault)
                .gearBoxDamage(gearBoxDamage)
                .engineDamage(engineDamage)
                .engineMguhWear(engineMguhWear)
                .engineEsWear(engineEsWear)
                .engineCeWear(engineCeWear)
                .engineIceWear(engineIceWear)
                .engineMgukWear(engineMgukWear)
                .engineTcWear(engineTcWear)
                .engineBlown(engineBlown)
                .engineSeized(engineSeized)
                .build();
    }
}
