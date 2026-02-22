package com.ua.yushchenko.f1.fastlaps.telemetry.processing;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;

import java.time.Instant;
import java.util.UUID;

/**
 * Centralized mock data and constants for unit tests.
 * Use this class to keep test classes clean and avoid inline test data duplication.
 */
public final class TestData {

    private TestData() {
    }

    // --- Constants ---
    public static final long SESSION_UID = 123456789L;
    public static final UUID SESSION_PUBLIC_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    public static final String SESSION_PUBLIC_ID_STR = SESSION_PUBLIC_ID.toString();
    /** F1 25: Race = 15. */
    public static final short SESSION_TYPE_RACE = 15;
    /** F1 25: Silverstone = 7. */
    public static final short TRACK_ID = 7;
    public static final Integer TRACK_LENGTH_M = 5000;
    public static final short TOTAL_LAPS = 58;
    public static final short AI_DIFFICULTY = 90;
    public static final Instant STARTED_AT = Instant.parse("2025-01-15T10:00:00Z");
    public static final Instant ENDED_AT = Instant.parse("2025-01-15T12:30:00Z");
    public static final String END_REASON = "EVENT_SEND";

    public static final short CAR_INDEX = 0;
    public static final short LAP_NUMBER = 1;
    public static final int LAP_TIME_MS = 87_500;
    public static final int SECTOR1_MS = 28_000;
    public static final int SECTOR2_MS = 30_500;
    public static final int SECTOR3_MS = 29_000;

    public static final Instant LAP_ENDED_AT = Instant.parse("2025-01-15T10:01:27Z");
    public static final Instant RAW_TS = Instant.parse("2025-01-15T10:01:00Z");
    public static final int FRAME_ID = 1000;
    public static final float LAP_DISTANCE_M = 1250.5f;
    public static final float THROTTLE = 0.95f;
    public static final float BRAKE = 0.0f;
    public static final short SPEED_KPH = 285;
    public static final short GEAR = 7;
    public static final int ENGINE_RPM = 11500;

    public static final float WEAR_FL = 5.2f;
    public static final float WEAR_FR = 5.0f;
    public static final float WEAR_RL = 4.8f;
    public static final float WEAR_RR = 5.1f;

    // --- Session ---
    public static Session session() {
        return Session.builder()
                .sessionUid(SESSION_UID)
                .publicId(SESSION_PUBLIC_ID)
                .sessionDisplayName(SESSION_PUBLIC_ID_STR)
                .packetFormat((short) 2025)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .sessionType(SESSION_TYPE_RACE)
                .trackId(TRACK_ID)
                .trackLengthM(TRACK_LENGTH_M)
                .totalLaps(TOTAL_LAPS)
                .aiDifficulty(AI_DIFFICULTY)
                .startedAt(STARTED_AT)
                .endedAt(ENDED_AT)
                .endReason(END_REASON)
                .createdAt(STARTED_AT)
                .updatedAt(ENDED_AT)
                .build();
    }

    /** Session without publicId (uses sessionUid for id string). */
    public static Session sessionWithoutPublicId() {
        return Session.builder()
                .sessionUid(SESSION_UID)
                .publicId(null)
                .sessionDisplayName(String.valueOf(SESSION_UID))
                .packetFormat((short) 2025)
                .sessionType(SESSION_TYPE_RACE)
                .trackId(TRACK_ID)
                .trackLengthM(TRACK_LENGTH_M)
                .createdAt(STARTED_AT)
                .updatedAt(ENDED_AT)
                .build();
    }

    // --- SessionRuntimeState ---
    public static SessionRuntimeState runtimeStateActive() {
        SessionRuntimeState state = new SessionRuntimeState(SESSION_UID);
        state.transitionTo(com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState.ACTIVE);
        return state;
    }

    public static SessionRuntimeState runtimeStateTerminal() {
        SessionRuntimeState state = new SessionRuntimeState(SESSION_UID);
        state.transitionTo(com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState.TERMINAL);
        return state;
    }

    public static SessionRuntimeState runtimeStateNull() {
        return null;
    }

    // --- CarSnapshot (inner class of SessionRuntimeState) ---
    public static SessionRuntimeState.CarSnapshot carSnapshot() {
        SessionRuntimeState.CarSnapshot snapshot = new SessionRuntimeState.CarSnapshot();
        snapshot.setSpeedKph((int) SPEED_KPH);
        snapshot.setGear((int) GEAR);
        snapshot.setEngineRpm(ENGINE_RPM);
        snapshot.setThrottle(THROTTLE);
        snapshot.setBrake(BRAKE);
        snapshot.setDrs(true);
        snapshot.setDrsAllowed(true);
        snapshot.setCurrentLap(1);
        snapshot.setCurrentSector(2);
        snapshot.setLapDistanceM(LAP_DISTANCE_M);
        snapshot.setCurrentLapTimeMs(45_000);
        snapshot.setBestLapTimeMs(43_000);
        snapshot.setErsEnergyPercent(75);
        snapshot.setErsDeployActive(false);
        snapshot.setErsDeployMode(2); // 2 = Hotlap (plan 11)
        snapshot.setTimestamp(RAW_TS);
        return snapshot;
    }

    // --- Lap ---
    public static Lap lap() {
        return Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber(LAP_NUMBER)
                .lapTimeMs(LAP_TIME_MS)
                .sector1TimeMs(SECTOR1_MS)
                .sector2TimeMs(SECTOR2_MS)
                .sector3TimeMs(SECTOR3_MS)
                .isInvalid(false)
                .penaltiesSeconds((short) 0)
                .positionAtLapStart(3)
                .endedAt(LAP_ENDED_AT)
                .build();
    }

    public static Lap lapInvalid() {
        Lap lap = lap();
        lap.setIsInvalid(true);
        return lap;
    }

    public static Lap lapZeroTime() {
        Lap lap = lap();
        lap.setLapTimeMs(0);
        return lap;
    }

    // --- SessionSummary ---
    public static SessionSummary sessionSummary() {
        return SessionSummary.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .totalLaps((short) 10)
                .bestLapTimeMs(87_200)
                .bestLapNumber((short) 3)
                .bestSector1Ms(27_900)
                .bestSector2Ms(30_400)
                .bestSector3Ms(28_900)
                .lastUpdatedAt(ENDED_AT)
                .build();
    }

    // --- CarTelemetryRaw ---
    public static CarTelemetryRaw carTelemetryRaw() {
        return CarTelemetryRaw.builder()
                .ts(RAW_TS)
                .sessionUid(SESSION_UID)
                .frameIdentifier(FRAME_ID)
                .carIndex(CAR_INDEX)
                .speedKph(SPEED_KPH)
                .throttle(THROTTLE)
                .brake(BRAKE)
                .lapDistanceM(LAP_DISTANCE_M)
                .lapNumber(LAP_NUMBER)
                .build();
    }

    // --- TyreWearPerLap ---
    public static TyreWearPerLap tyreWearPerLap() {
        return TyreWearPerLap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber(LAP_NUMBER)
                .wearFL(WEAR_FL)
                .wearFR(WEAR_FR)
                .wearRL(WEAR_RL)
                .wearRR(WEAR_RR)
                .compound((short) 18)
                .build();
    }

    // --- TyreWearSnapshot ---
    public static TyreWearSnapshot tyreWearSnapshot() {
        return new TyreWearSnapshot(WEAR_FL, WEAR_FR, WEAR_RL, WEAR_RR);
    }
}
