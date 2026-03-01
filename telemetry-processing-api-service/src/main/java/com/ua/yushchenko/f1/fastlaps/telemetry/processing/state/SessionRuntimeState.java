package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.CarPositionDto;
import lombok.Data;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    /** Last known race position per car (from LapData m_carPosition). Persisted as finishing position on session end. */
    private final Map<Integer, Integer> lastCarPositionByCarIndex = new ConcurrentHashMap<>();

    /** Latest world position (x, z) per car from Motion (B9). Index 0 = worldPosX, 1 = worldPosZ. */
    private final Map<Integer, float[]> latestWorldPositionByCarIndex = new ConcurrentHashMap<>();

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
     * Update last known race position for a car (from LapData m_carPosition). Used to persist finishing position on session end.
     */
    public void setLastCarPosition(int carIndex, int position) {
        lastCarPositionByCarIndex.put(carIndex, position);
        this.lastSeenAt = Instant.now();
    }

    /**
     * Get last known race position for a car, or null if never received.
     */
    public Integer getLastCarPosition(int carIndex) {
        return lastCarPositionByCarIndex.get(carIndex);
    }

    /**
     * Get last known race position for the player car (playerCarIndex), or null.
     */
    public Integer getLastCarPositionForPlayer() {
        if (playerCarIndex == null) {
            return null;
        }
        return lastCarPositionByCarIndex.get(playerCarIndex.intValue());
    }

    /**
     * Update latest world position for a car (from Motion packet, B9).
     */
    public void updatePosition(int carIndex, float worldPosX, float worldPosZ) {
        latestWorldPositionByCarIndex.put(carIndex, new float[]{worldPosX, worldPosZ});
        this.lastSeenAt = Instant.now();
    }

    /**
     * Get latest positions for all cars (B9 Live Track Map). Returns list ordered by carIndex.
     */
    public List<CarPositionDto> getLatestPositions() {
        return latestWorldPositionByCarIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> CarPositionDto.builder()
                        .carIndex(e.getKey())
                        .worldPosX(e.getValue()[0])
                        .worldPosZ(e.getValue()[1])
                        .build())
                .collect(Collectors.toList());
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
     * Get the latest car snapshot together with its car index (for consistent best-lap lookup).
     * Selects by player car index when set; otherwise by most recent timestamp.
     * Caller must use the returned car index when loading SessionSummary so snapshot and best-lap
     * refer to the same car (avoids mixing data when playerCarIndex is unset and fallback is used).
     *
     * @return entry with key = carIndex, value = snapshot; or null if no snapshots
     */
    public Map.Entry<Integer, CarSnapshot> getLatestCarSnapshotWithCarIndex() {
        if (snapshots.isEmpty()) {
            return null;
        }
        if (playerCarIndex != null) {
            CarSnapshot snapshot = snapshots.get(playerCarIndex);
            if (snapshot != null) {
                return Map.entry(playerCarIndex.intValue(), snapshot);
            }
        }
        return snapshots.entrySet().stream()
                .max(Comparator.comparing(e -> e.getValue().getTimestamp(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .orElse(null);
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
        /** DRS wing open (from Car Telemetry m_drs). Set in CarTelemetryProcessor. */
        private Boolean drs;
        /** DRS zone allowed (from Car Status m_drsAllowed). Set in CarStatusProcessor. Plan 12. */
        private Boolean drsAllowed;
        private Integer currentLap;
        private Integer currentSector;
        /** Lap distance in metres (from LapData); used for pedal trace. */
        private Float lapDistanceM;
        /** Current lap time in ms (from LapData m_currentLapTimeInMS). Used for delta to best. */
        private Integer currentLapTimeMs;
        /** Best lap time in session (ms). Enriched from SessionSummary when building WS snapshot. */
        private Integer bestLapTimeMs;
        /** ERS energy 0–100%. Set from CarStatusProcessor (ersStoreEnergy / ERS_MAX_J). */
        private Integer ersEnergyPercent;
        /** ERS deploy active (ersDeployMode != NONE). Set from CarStatusProcessor. */
        private Boolean ersDeployActive;
        /** ERS deploy mode code (0=none, 1=medium, 2=hotlap, 3=overtake); for display name in WS. */
        private Integer ersDeployMode;
        private Instant timestamp;
        /** Tyre surface temperatures °C, order RL, RR, FL, FR. From CarTelemetry. */
        private int[] tyresSurfaceTempC;
        /** Fuel remaining 0–100%. From CarStatus fuelInTank / fuelCapacity. */
        private Integer fuelRemainingPercent;
        /** Visual tyre compound (F1 25 code). From CarStatus; used for leaderboard S/M/H display. */
        private Integer visualTyreCompound;
        /** Session time in seconds (from packet header). For live UI display. */
        private Float sessionTimeSeconds;
    }
}
