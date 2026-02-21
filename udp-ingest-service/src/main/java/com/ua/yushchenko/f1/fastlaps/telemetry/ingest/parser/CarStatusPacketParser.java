package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 CarStatusData from ByteBuffer into {@link CarStatusDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (55 bytes per car).
 */
@Component
public class CarStatusPacketParser {

    /** Size in bytes of one CarStatusData struct (F1 25 spec). */
    public static final int CAR_STATUS_DATA_SIZE_BYTES = 55;

    /**
     * Parse one car's status from current buffer position. Buffer must be positioned at start of CarStatusData.
     * Advances buffer position by {@value #CAR_STATUS_DATA_SIZE_BYTES} bytes.
     */
    public CarStatusDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int tractionControl = buffer.get() & 0xFF;
        int antiLockBrakes = buffer.get() & 0xFF;
        int fuelMix = buffer.get() & 0xFF;
        int frontBrakeBias = buffer.get() & 0xFF;
        int pitLimiterStatus = buffer.get() & 0xFF;
        float fuelInTank = buffer.getFloat();
        float fuelCapacity = buffer.getFloat();
        float fuelRemainingLaps = buffer.getFloat();
        int maxRpm = buffer.getShort() & 0xFFFF;
        int idleRpm = buffer.getShort() & 0xFFFF;
        int maxGears = buffer.get() & 0xFF;
        int drsAllowed = buffer.get() & 0xFF;
        int drsActivationDistance = buffer.getShort() & 0xFFFF;
        int actualTyreCompound = buffer.get() & 0xFF;
        int visualTyreCompound = buffer.get() & 0xFF;
        int tyresAgeLaps = buffer.get() & 0xFF;
        int vehicleFiaFlags = buffer.get();
        float enginePowerIce = buffer.getFloat();
        float enginePowerMguk = buffer.getFloat();
        float ersStoreEnergy = buffer.getFloat();
        int ersDeployMode = buffer.get() & 0xFF;
        float ersHarvestedThisLapMguk = buffer.getFloat();
        float ersHarvestedThisLapMguh = buffer.getFloat();
        float ersDeployedThisLap = buffer.getFloat();
        int networkPaused = buffer.get() & 0xFF;

        return CarStatusDto.builder()
                .tractionControl(tractionControl)
                .abs(antiLockBrakes)
                .fuelInTank(fuelInTank)
                .fuelMix(fuelMix)
                .drsAllowed(drsAllowed == 1)
                .tyresCompound(actualTyreCompound)
                .tyresAgeLaps(tyresAgeLaps)
                .ersStoreEnergy(ersStoreEnergy)
                .frontBrakeBias(frontBrakeBias)
                .pitLimiterStatus(pitLimiterStatus)
                .fuelCapacity(fuelCapacity)
                .fuelRemainingLaps(fuelRemainingLaps)
                .maxRpm(maxRpm)
                .idleRpm(idleRpm)
                .maxGears(maxGears)
                .drsActivationDistance(drsActivationDistance)
                .visualTyreCompound(visualTyreCompound)
                .vehicleFiaFlags(vehicleFiaFlags)
                .enginePowerIce(enginePowerIce)
                .enginePowerMguk(enginePowerMguk)
                .ersDeployMode(ersDeployMode)
                .ersHarvestedThisLapMguk(ersHarvestedThisLapMguk)
                .ersHarvestedThisLapMguh(ersHarvestedThisLapMguh)
                .ersDeployedThisLap(ersDeployedThisLap)
                .networkPaused(networkPaused)
                .build();
    }
}
