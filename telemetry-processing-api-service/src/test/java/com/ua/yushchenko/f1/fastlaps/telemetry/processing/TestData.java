package com.ua.yushchenko.f1.fastlaps.telemetry.processing;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.model.Point3D;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;

import java.time.Instant;
import java.util.List;
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

    /** Fuel in tank (kg) for fuel-by-lap tests. */
    public static final float FUEL_AT_LAP_1_END = 98.5f;
    public static final float FUEL_AT_LAP_2_END = 95.0f;
    /** ERS store energy (J) for ers-by-lap; ~62.5% of 4M J. */
    public static final float ERS_STORE_AT_LAP_1_END = 2_500_000f;
    public static final float ERS_STORE_AT_LAP_2_END = 2_200_000f;

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
        snapshot.setTyresSurfaceTempC(new int[]{95, 99, 102, 98}); // RL, RR, FL, FR °C
        snapshot.setFuelRemainingPercent(67);
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

    /** Lap with given number and time (for pit/stint scenarios). */
    public static Lap lapWithNumber(int lapNumber, Integer lapTimeMs) {
        return Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) lapNumber)
                .lapTimeMs(lapTimeMs)
                .sector1TimeMs(SECTOR1_MS)
                .sector2TimeMs(SECTOR2_MS)
                .sector3TimeMs(SECTOR3_MS)
                .isInvalid(false)
                .positionAtLapStart(null)
                .endedAt(LAP_ENDED_AT)
                .build();
    }

    /** Two laps (1 and 2) for one pit stop scenario: compound change 18→16 at lap 2. */
    public static java.util.List<Lap> lapsForPitScenario() {
        return java.util.List.of(
                lapWithNumber(1, 92_500),
                lapWithNumber(2, 95_200)
        );
    }

    /** Ten laps (1–10) for two-stint scenario. */
    public static java.util.List<Lap> lapsForStintScenario() {
        java.util.List<Lap> list = new java.util.ArrayList<>();
        for (int n = 1; n <= 10; n++) {
            list.add(lapWithNumber(n, 87_000 + n * 100));
        }
        return list;
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

    /** Second car index for comparison tests. */
    public static final short CAR_INDEX_1 = 1;

    /** Session summary for car index 1 (Block G — comparison). */
    public static SessionSummary sessionSummaryCar1() {
        return SessionSummary.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .totalLaps((short) 10)
                .bestLapTimeMs(87_500)
                .bestLapNumber((short) 5)
                .bestSector1Ms(28_100)
                .bestSector2Ms(30_600)
                .bestSector3Ms(28_800)
                .lastUpdatedAt(ENDED_AT)
                .build();
    }

    /** Lap for car 1. */
    public static Lap lapCar1() {
        return Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(87_600)
                .sector1TimeMs(28_200)
                .sector2TimeMs(30_700)
                .sector3TimeMs(28_700)
                .isInvalid(false)
                .positionAtLapStart(2)
                .endedAt(LAP_ENDED_AT)
                .build();
    }

    /** Session finishing position (P1, P2) for participants displayLabel. */
    public static SessionFinishingPosition finishingPositionP1() {
        return SessionFinishingPosition.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .finishingPosition(1)
                .build();
    }

    public static SessionFinishingPosition finishingPositionP2() {
        return SessionFinishingPosition.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .finishingPosition(2)
                .build();
    }

    // --- CarStatusRaw (fuel/ERS by lap) ---
    /** CarStatusRaw at given timestamp with fuel and ERS (for fuel-by-lap / ers-by-lap tests). */
    public static CarStatusRaw carStatusRaw(Instant ts, int frameId, Float fuelKg, Float ersStoreEnergy) {
        return CarStatusRaw.builder()
                .ts(ts)
                .sessionUid(SESSION_UID)
                .frameIdentifier(frameId)
                .carIndex(CAR_INDEX)
                .fuelInTank(fuelKg)
                .ersStoreEnergy(ersStoreEnergy)
                .build();
    }

    /** Two laps with endedAt for fuel/ERS-by-lap scenario. */
    public static java.util.List<Lap> lapsWithEndedAtForFuelErs() {
        Instant lap1End = Instant.parse("2025-01-15T10:01:27Z");
        Instant lap2End = Instant.parse("2025-01-15T10:03:00Z");
        return java.util.List.of(
                Lap.builder()
                        .sessionUid(SESSION_UID)
                        .carIndex(CAR_INDEX)
                        .lapNumber((short) 1)
                        .lapTimeMs(87_000)
                        .endedAt(lap1End)
                        .isInvalid(false)
                        .build(),
                Lap.builder()
                        .sessionUid(SESSION_UID)
                        .carIndex(CAR_INDEX)
                        .lapNumber((short) 2)
                        .lapTimeMs(93_000)
                        .endedAt(lap2End)
                        .isInvalid(false)
                        .build()
        );
    }

    /** CarStatusRaw rows near lap end times for fuel/ERS-by-lap (match lapsWithEndedAtForFuelErs). */
    public static java.util.List<CarStatusRaw> carStatusRawForFuelErsByLap() {
        Instant lap1End = Instant.parse("2025-01-15T10:01:27Z");
        Instant lap2End = Instant.parse("2025-01-15T10:03:00Z");
        return java.util.List.of(
                carStatusRaw(lap1End, 1001, FUEL_AT_LAP_1_END, ERS_STORE_AT_LAP_1_END),
                carStatusRaw(lap2End, 1002, FUEL_AT_LAP_2_END, ERS_STORE_AT_LAP_2_END)
        );
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
    /** F1 25 compound 18 (e.g. C3 medium). */
    public static final short COMPOUND_18 = 18;
    /** F1 25 compound 16 (e.g. C5 soft). */
    public static final short COMPOUND_16 = 16;

    public static TyreWearPerLap tyreWearPerLap() {
        return TyreWearPerLap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber(LAP_NUMBER)
                .wearFL(WEAR_FL)
                .wearFR(WEAR_FR)
                .wearRL(WEAR_RL)
                .wearRR(WEAR_RR)
                .compound(COMPOUND_18)
                .build();
    }

    /** Tyre wear for lap 1 (compound 18) and lap 2 (compound 16) — one pit stop between them. */
    public static java.util.List<TyreWearPerLap> tyreWearTwoLapsOnePit() {
        return java.util.List.of(
                TyreWearPerLap.builder()
                        .sessionUid(SESSION_UID)
                        .carIndex(CAR_INDEX)
                        .lapNumber((short) 1)
                        .wearFL(0.02f)
                        .wearFR(0.02f)
                        .wearRL(0.03f)
                        .wearRR(0.03f)
                        .compound(COMPOUND_18)
                        .build(),
                TyreWearPerLap.builder()
                        .sessionUid(SESSION_UID)
                        .carIndex(CAR_INDEX)
                        .lapNumber((short) 2)
                        .wearFL(0.01f)
                        .wearFR(0.01f)
                        .wearRL(0.01f)
                        .wearRR(0.01f)
                        .compound(COMPOUND_16)
                        .build()
        );
    }

    /** Stint scenario: laps 1–5 compound 18, laps 6–10 compound 16 (two stints). */
    public static java.util.List<TyreWearPerLap> tyreWearTwoStints() {
        java.util.List<TyreWearPerLap> list = new java.util.ArrayList<>();
        for (int lap = 1; lap <= 5; lap++) {
            list.add(TyreWearPerLap.builder()
                    .sessionUid(SESSION_UID)
                    .carIndex(CAR_INDEX)
                    .lapNumber((short) lap)
                    .wearFL(0.02f)
                    .wearFR(0.02f)
                    .wearRL(0.03f)
                    .wearRR(0.03f)
                    .compound(COMPOUND_18)
                    .build());
        }
        for (int lap = 6; lap <= 10; lap++) {
            list.add(TyreWearPerLap.builder()
                    .sessionUid(SESSION_UID)
                    .carIndex(CAR_INDEX)
                    .lapNumber((short) lap)
                    .wearFL(0.01f)
                    .wearFR(0.01f)
                    .wearRL(0.01f)
                    .wearRR(0.01f)
                    .compound(COMPOUND_16)
                    .build());
        }
        return list;
    }

    // --- TyreWearSnapshot ---
    public static TyreWearSnapshot tyreWearSnapshot() {
        return new TyreWearSnapshot(WEAR_FL, WEAR_FR, WEAR_RL, WEAR_RR);
    }

    // --- SessionDriver ---
    public static SessionDriver sessionDriver() {
        return SessionDriver.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .driverLabel("VER")
                .createdAt(STARTED_AT)
                .updatedAt(ENDED_AT)
                .build();
    }

    // --- SessionEvent ---
    public static SessionEvent sessionEvent() {
        return SessionEvent.builder()
                .id(1L)
                .sessionUid(SESSION_UID)
                .frameId(FRAME_ID)
                .lap((short) 24)
                .eventCode("FTLP")
                .carIndex(CAR_INDEX)
                .detail("{\"vehicleIdx\":0,\"lapTime\":84.532}")
                .createdAt(RAW_TS)
                .build();
    }

    /** Silverstone layout (track_id 8) for Block F — B8. */
    public static final short TRACK_LAYOUT_TRACK_ID = 8;
    public static final List<Point3D> TRACK_LAYOUT_POINTS = List.of(
            new Point3D(100.0, 0.0, 300.0),
            new Point3D(250.0, 0.0, 100.0),
            new Point3D(700.0, 0.0, 250.0),
            new Point3D(100.0, 0.0, 300.0)
    );

    public static TrackLayout trackLayout() {
        return TrackLayout.builder()
                .trackId(TRACK_LAYOUT_TRACK_ID)
                .points(TRACK_LAYOUT_POINTS)
                .version((short) 1)
                .minX(100.0)
                .minY(100.0)
                .maxX(700.0)
                .maxY(500.0)
                .build();
    }
}
