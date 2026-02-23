package com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner;

import lombok.Value;

/**
 * One detected corner segment: start/end/apex distances (metres).
 * Apex is the distance at which speed is minimum within the segment.
 */
@Value
public class CornerSegment {

    float startDistanceM;
    float endDistanceM;
    float apexDistanceM;
}
