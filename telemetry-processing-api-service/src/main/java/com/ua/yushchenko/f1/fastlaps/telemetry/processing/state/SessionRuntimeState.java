package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Runtime state for a single session (thread-safe).
 * Tracks current state, watermarks per carIndex, and lifecycle metadata.
 * See: implementation_steps_plan.md § Етап 4.3.
 */
@Data
public class SessionRuntimeState {

    private final long sessionUID;
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    // FSM state
    private volatile SessionState state = SessionState.INIT;
    private volatile EndReason endReason;

    // Lifecycle timestamps
    private volatile Instant startedAt;
    private volatile Instant endedAt;
    private volatile Instant lastSeenAt;

    // Watermarks: per carIndex, tracks highest processed frameIdentifier
    private final Map<Integer, AtomicInteger> watermarks = new ConcurrentHashMap<>();

    // Snapshot for WebSocket (per carIndex)
    private final Map<Integer, CarSnapshot> snapshots = new ConcurrentHashMap<>();

    public SessionRuntimeState(long sessionUID) {
        this.sessionUID = sessionUID;
        this.lastSeenAt = Instant.now();
    }

    /**
     * Transition to new state (thread-safe).
     */
    public void transitionTo(SessionState newState) {
        stateLock.writeLock().lock();
        try {
            this.state = newState;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Update watermark for carIndex (idempotent, only increases).
     */
    public void updateWatermark(int carIndex, int frameIdentifier) {
        watermarks.computeIfAbsent(carIndex, k -> new AtomicInteger(0))
                .updateAndGet(current -> Math.max(current, frameIdentifier));
        this.lastSeenAt = Instant.now();
    }

    /**
     * Get current watermark for carIndex.
     */
    public int getWatermark(int carIndex) {
        AtomicInteger watermark = watermarks.get(carIndex);
        return watermark != null ? watermark.get() : 0;
    }

    /**
     * Update snapshot for WebSocket live data.
     */
    public void updateSnapshot(int carIndex, CarSnapshot snapshot) {
        snapshots.put(carIndex, snapshot);
        this.lastSeenAt = Instant.now();
    }

    /**
     * Get snapshot for WebSocket.
     */
    public CarSnapshot getSnapshot(int carIndex) {
        return snapshots.get(carIndex);
    }

    /**
     * Check if session is in terminal state.
     */
    public boolean isTerminal() {
        return state == SessionState.TERMINAL;
    }

    /**
     * Check if session is active.
     */
    public boolean isActive() {
        return state == SessionState.ACTIVE;
    }

    /**
     * Get latest snapshot for all cars (for WebSocket broadcast).
     * Returns null if no snapshots available.
     */
    public com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage getLatestSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        // For MVP, return first available snapshot (carIndex 0 typically)
        CarSnapshot snapshot = snapshots.get(0);
        return com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.WsSnapshotMessageBuilder.build(snapshot);
    }

    /**
     * Simple snapshot holder for live telemetry data.
     */
    @Data
    public static class CarSnapshot {
        private Integer speedKph;
        private Integer gear;
        private Integer engineRpm;
        private Float throttle;
        private Float brake;
        /** DRS active; set from CarStatusConsumer */
        private Boolean drs;
        private Integer currentLap;
        private Integer currentSector;
        /** Lap distance in metres (from LapData); used for pedal trace. */
        private Float lapDistanceM;
        private Instant timestamp;
    }
}
