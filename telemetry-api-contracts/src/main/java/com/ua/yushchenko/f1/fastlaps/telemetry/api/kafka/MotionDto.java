package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Motion data for one car (from PacketMotionData / CarMotionData).
 * Used for curvature/yaw-based corner detection. Plan: 13-session-summary-speed-corner-graph.md Phase 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotionDto {

    private float worldPositionX;
    private float worldPositionY;
    private float worldPositionZ;
    private float gForceLateral;
    private float yaw;
}
