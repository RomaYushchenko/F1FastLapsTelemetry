package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory state for automatic track layout recording based on Motion packets.
 * One instance is stored inside SessionRuntimeState per active session.
 */
@Data
public class TrackRecordingState {

    public enum Status {
        IDLE,
        WAITING_FOR_LAP_START,
        RECORDING,
        SAVING,
        DONE,
        ABORTED
    }

    private volatile Status status = Status.IDLE;
    private final List<PointXYZD> buffer = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private short trackId = -1;

    /**
     * When lap-complete is received before enough motion frames (cross-topic ordering),
     * we defer save until buffer reaches MIN_POINTS_WHEN_LAP_COMPLETE.
     */
    private volatile boolean pendingLapComplete;
    private volatile boolean pendingLapInvalid;

    /**
     * Returns true when this motion frame should be sampled into the buffer.
     * Uses SAMPLE_EVERY = 5 → ~12 Hz at 60 FPS.
     */
    public boolean shouldSample() {
        return frameCounter.incrementAndGet() % 5 == 0;
    }

    /**
     * Append a new sampled point to the recording buffer.
     */
    public void addPoint(float x, float y, float z, float lapDistance) {
        buffer.add(new PointXYZD(x, y, z, lapDistance));
    }

    /**
     * Clear buffer and reset sampling counter for a new attempt.
     */
    public void reset() {
        buffer.clear();
        frameCounter.set(0);
        pendingLapComplete = false;
        pendingLapInvalid = false;
    }
}

