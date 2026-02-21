package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * Parses F1 25 CarStatusData from ByteBuffer into {@link CarStatusDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (55 bytes per car).
 */
@Component
public class CarStatusPacketParser {

    private static final int CAR_STATUS_TAIL_SKIP = 14;

    /**
     * Parse one car's status from current buffer position. Buffer must be positioned at start of CarStatusData.
     * Advances buffer position by consumed bytes (full CarStatusData).
     */
    public CarStatusDto parse(ByteBuffer buffer) {
        int tractionControl = buffer.get() & 0xFF;
        int antiLockBrakes = buffer.get() & 0xFF;
        int fuelMix = buffer.get() & 0xFF;
        buffer.get(); // frontBrakeBias
        buffer.get(); // pitLimiterStatus
        float fuelInTank = buffer.getFloat();
        float fuelCapacity = buffer.getFloat();
        float fuelRemainingLaps = buffer.getFloat();
        buffer.getShort(); // maxRPM
        buffer.getShort(); // idleRPM
        buffer.get();     // maxGears
        int drsAllowed = buffer.get() & 0xFF;
        buffer.getShort(); // drsActivationDistance
        int actualTyreCompound = buffer.get() & 0xFF;
        buffer.get();     // visualTyreCompound
        int tyresAgeLaps = buffer.get() & 0xFF;
        buffer.get();     // vehicleFiaFlags
        buffer.getFloat(); // enginePowerICE
        buffer.getFloat(); // enginePowerMGUK
        float ersStoreEnergy = buffer.getFloat();
        if (buffer.remaining() >= CAR_STATUS_TAIL_SKIP) {
            buffer.position(buffer.position() + CAR_STATUS_TAIL_SKIP);
        }

        return CarStatusDto.builder()
                .tractionControl(tractionControl)
                .abs(antiLockBrakes)
                .fuelInTank(fuelInTank)
                .fuelMix(fuelMix)
                .drsAllowed(drsAllowed == 1)
                .tyresCompound(actualTyreCompound)
                .tyresAgeLaps(tyresAgeLaps)
                .ersStoreEnergy(ersStoreEnergy)
                .build();
    }
}
