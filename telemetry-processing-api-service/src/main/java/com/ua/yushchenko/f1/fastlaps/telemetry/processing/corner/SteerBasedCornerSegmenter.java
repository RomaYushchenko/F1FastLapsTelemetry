package com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects corner segments from ordered (distanceM, speedKph, steer) points using steer threshold.
 * Signal S(d) = |steer|; S > steerOn starts a corner, S < steerOff ends it (hysteresis).
 * Apex = distance at which speed is minimum within the segment.
 * Plan: 13-session-summary-speed-corner-graph.md Phase 2.
 */
@Slf4j
@Component
public class SteerBasedCornerSegmenter {

    private static final float DEFAULT_STEER_ON = 0.05f;
    private static final float DEFAULT_STEER_OFF = 0.03f;
    /** Minimum segment length in metres to avoid noise. */
    private static final float MIN_SEGMENT_LENGTH_M = 20f;

    @Value("${f1.corner.steer-on:" + DEFAULT_STEER_ON + "}")
    private float steerOn = DEFAULT_STEER_ON;

    @Value("${f1.corner.steer-off:" + DEFAULT_STEER_OFF + "}")
    private float steerOff = DEFAULT_STEER_OFF;

    /**
     * One point with distance, speed and steer for segmenter input.
     */
    public record Point(float distanceM, int speedKph, float steer) {}

    /**
     * Detect corner segments from ordered points. Points must be sorted by distanceM.
     */
    public List<CornerSegment> detect(List<Point> points) {
        if (points == null || points.size() < 2) {
            return List.of();
        }
        List<CornerSegment> segments = new ArrayList<>();
        boolean inCorner = false;
        int segmentStartIdx = -1;

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            float absSteer = Math.abs(p.steer());

            if (!inCorner && absSteer > steerOn) {
                inCorner = true;
                segmentStartIdx = i;
            } else if (inCorner && absSteer < steerOff) {
                inCorner = false;
                if (segmentStartIdx >= 0 && i > segmentStartIdx) {
                    CornerSegment seg = buildSegment(points, segmentStartIdx, i);
                    if (seg != null) {
                        segments.add(seg);
                    }
                }
                segmentStartIdx = -1;
            }
        }
        if (inCorner && segmentStartIdx >= 0 && segmentStartIdx < points.size() - 1) {
            CornerSegment seg = buildSegment(points, segmentStartIdx, points.size() - 1);
            if (seg != null) {
                segments.add(seg);
            }
        }

        log.debug("detect: {} points -> {} segments", points.size(), segments.size());
        return segments;
    }

    private CornerSegment buildSegment(List<Point> points, int startIdx, int endIdx) {
        float startM = points.get(startIdx).distanceM();
        float endM = points.get(endIdx).distanceM();
        if (endM - startM < MIN_SEGMENT_LENGTH_M) {
            return null;
        }
        int minSpeedIdx = startIdx;
        int minSpeed = points.get(startIdx).speedKph();
        for (int i = startIdx + 1; i <= endIdx; i++) {
            int s = points.get(i).speedKph();
            if (s < minSpeed) {
                minSpeed = s;
                minSpeedIdx = i;
            }
        }
        float apexM = points.get(minSpeedIdx).distanceM();
        return new CornerSegment(startM, endM, apexM);
    }
}
