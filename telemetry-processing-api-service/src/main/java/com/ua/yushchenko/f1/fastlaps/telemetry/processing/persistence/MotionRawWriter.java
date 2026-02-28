package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.MotionRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.MotionRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Writes motion samples to motion_raw. Called from MotionConsumer.
 * Plan: 13-session-summary-speed-corner-graph.md Phase 4.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotionRawWriter {

    private final MotionRawRepository repository;

    public void write(
            Instant ts,
            long sessionUid,
            int frameIdentifier,
            short carIndex,
            Float gForceLateral,
            Float yaw,
            Float worldPosX,
            Float worldPosZ
    ) {
        try {
            MotionRaw row = MotionRaw.builder()
                    .ts(ts)
                    .sessionUid(sessionUid)
                    .frameIdentifier(frameIdentifier)
                    .carIndex(carIndex)
                    .gForceLateral(gForceLateral)
                    .yaw(yaw)
                    .worldPosX(worldPosX)
                    .worldPosZ(worldPosZ)
                    .build();
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to write motion_raw: sessionUid={}, frame={}, carIndex={}",
                    sessionUid, frameIdentifier, carIndex, e);
        }
    }
}
