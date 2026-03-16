package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

/**
 * Single sampled world position on the recorded track.
 *
 * x = worldPositionX (horizontal left/right)
 * y = worldPositionY (elevation above track surface)
 * z = worldPositionZ (horizontal forward/backward)
 * lapDistance = distance around the lap at the time of sampling (metres)
 */
public record PointXYZD(float x, float y, float z, float lapDistance) {
}

