package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import lombok.Data;

import java.time.Instant;
import java.util.Comparator;
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

    /**
     * Watermarks per packet type and carIndex, to allow out-of-order arrival across Kafka topics.
     * Each topic (lap data, car telemetry, car status) can advance independently.
     */
    private final Map<Integer, AtomicInteger> lapWatermarks = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> telemetryWatermarks = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> statusWatermarks = new ConcurrentHashMap<>();

    /** Current player car index (from packet headers); used to select correct snapshot when multiple exist. */
    private volatile Integer playerCarIndex;

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
     * Lap data: get/update watermark for carIndex (idempotent, only increases).
     */
    public int getLapWatermark(int carIndex) {
        AtomicInteger w = lapWatermarks.get(carIndex);
        return w != null ? w.get() : 0;
    }

    public void updateLapWatermark(int carIndex, int frameIdentifier) {
        lapWatermarks.computeIfAbsent(carIndex, k -> new AtomicInteger(0))
                .updateAndGet(current -> Math.max(current, frameIdentifier));
        this.lastSeenAt = Instant.now();
    }

    /**
     * Car telemetry: get/update watermark for carIndex.
     */
    public int getTelemetryWatermark(int carIndex) {
        AtomicInteger w = telemetryWatermarks.get(carIndex);
        return w != null ? w.get() : 0;
    }

    public void updateTelemetryWatermark(int carIndex, int frameIdentifier) {
        telemetryWatermarks.computeIfAbsent(carIndex, k -> new AtomicInteger(0))
                .updateAndGet(current -> Math.max(current, frameIdentifier));
        this.lastSeenAt = Instant.now();
    }

    /**
     * Car status: get/update watermark for carIndex.
     */
    public int getStatusWatermark(int carIndex) {
        AtomicInteger w = statusWatermarks.get(carIndex);
        return w != null ? w.get() : 0;
    }

    public void updateStatusWatermark(int carIndex, int frameIdentifier) {
        statusWatermarks.computeIfAbsent(carIndex, k -> new AtomicInteger(0))
                .updateAndGet(current -> Math.max(current, frameIdentifier));
        this.lastSeenAt = Instant.now();
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
     * Set current player car index (from packet headers). Used to select the correct snapshot when
     * multiple car indices exist (e.g. after mid-session car switch). Kept in sync with Session entity.
     */
    public void setPlayerCarIndex(int carIndex) {
        this.playerCarIndex = carIndex;
    }

    /**
     * Get latest snapshot for WebSocket broadcast.
     * Selects by current player car index when set; otherwise by most recent timestamp to avoid
     * undefined map iteration order (e.g. ConcurrentHashMap) when multiple snapshots exist.
     */
    public com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage getLatestSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        CarSnapshot snapshot = null;
        if (playerCarIndex != null) {
            snapshot = snapshots.get(playerCarIndex);
        }
        if (snapshot == null) {
            snapshot = snapshots.entrySet().stream()
                    .max(Comparator.comparing(e -> e.getValue().getTimestamp(), Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }
        return com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.WsSnapshotMessageBuilder.build(snapshot);
    }

    /**
     * Get the latest car snapshot for the current player (or most recent) without building WS message.
     * Used by LiveDataBroadcaster to enrich with bestLapTimeMs before building WsSnapshotMessage.
     */
    public CarSnapshot getLatestCarSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        CarSnapshot snapshot = null;
        if (playerCarIndex != null) {
            snapshot = snapshots.get(playerCarIndex);
        }
        if (snapshot == null) {
            snapshot = snapshots.entrySet().stream()
                    .max(Comparator.comparing(e -> e.getValue().getTimestamp(), Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }
        return snapshot;
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
        /** Current lap time in ms (from LapData m_currentLapTimeInMS). Used for delta to best. */
        private Integer currentLapTimeMs;
        /** Best lap time in session (ms). Enriched from SessionSummary when building WS snapshot. */
        private Integer bestLapTimeMs;
        private Instant timestamp;
    }
}
