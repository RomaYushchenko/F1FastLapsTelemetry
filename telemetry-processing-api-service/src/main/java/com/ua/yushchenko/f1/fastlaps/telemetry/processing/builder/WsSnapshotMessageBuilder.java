package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.CarStatusMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapDataMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState.CarSnapshot;

/**
 * Builds WsSnapshotMessage from CarSnapshot (assembly only, no business logic).
 * Static so SessionRuntimeState (non-Spring) can call without DI.
 * See: implementation_phases.md Phase 4.1.
 */
public final class WsSnapshotMessageBuilder {

    private WsSnapshotMessageBuilder() {
    }

    /**
     * Build WebSocket snapshot message from a car snapshot.
     *
     * @param snapshot snapshot for one car (e.g. carIndex 0); null returns null
     */
    public static WsSnapshotMessage build(CarSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        Integer currentLapTimeMs = snapshot.getCurrentLapTimeMs();
        Integer bestLapTimeMs = snapshot.getBestLapTimeMs();
        Integer deltaMs = null;
        if (currentLapTimeMs != null && bestLapTimeMs != null) {
            deltaMs = currentLapTimeMs - bestLapTimeMs;
        }

        return WsSnapshotMessage.builder()
                .type(WsSnapshotMessage.TYPE)
                .timestamp(snapshot.getTimestamp())
                .speedKph(snapshot.getSpeedKph())
                .gear(snapshot.getGear())
                .engineRpm(snapshot.getEngineRpm())
                .throttle(snapshot.getThrottle())
                .brake(snapshot.getBrake())
                .drs(snapshot.getDrs())
                .drsAllowed(snapshot.getDrsAllowed())
                .currentLap(snapshot.getCurrentLap())
                .currentSector(snapshot.getCurrentSector())
                .currentSectorDisplayName(LapDataMapper.sectorDisplayName(snapshot.getCurrentSector()))
                .currentLapTimeMs(currentLapTimeMs)
                .bestLapTimeMs(bestLapTimeMs)
                .deltaMs(deltaMs)
                .ersEnergyPercent(snapshot.getErsEnergyPercent())
                .ersDeployActive(snapshot.getErsDeployActive())
                .ersDeployModeDisplayName(CarStatusMapper.ersDeployModeDisplayName(snapshot.getErsDeployMode()))
                .tyresSurfaceTempC(snapshot.getTyresSurfaceTempC() != null ? snapshot.getTyresSurfaceTempC().clone() : null)
                .fuelRemainingPercent(snapshot.getFuelRemainingPercent())
                .build();
    }
}

