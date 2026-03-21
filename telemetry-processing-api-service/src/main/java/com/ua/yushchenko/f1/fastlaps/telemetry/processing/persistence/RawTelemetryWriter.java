package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Writes raw car telemetry samples to car_telemetry_raw for pedal trace.
 * See: PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RawTelemetryWriter {

    private final CarTelemetryRawRepository repository;

    /**
     * Persist one telemetry sample. Called from CarTelemetryProcessor for legacy single-car events.
     */
    public void write(
            Instant ts,
            long sessionUid,
            int frameIdentifier,
            short carIndex,
            Integer speedKph,
            Float throttle,
            Float brake,
            Float sessionTimeS,
            Short lapNumber,
            Float lapDistanceM
    ) {
        CarTelemetryRaw row = buildRow(
                ts, sessionUid, frameIdentifier, carIndex,
                speedKph, throttle, brake, sessionTimeS, lapNumber, lapDistanceM);
        save(row);
    }

    /**
     * Batch insert for one UDP frame (all cars). Uses a single {@code saveAll} to reduce DB round-trips.
     */
    public void saveAll(List<CarTelemetryRaw> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try {
            repository.saveAll(rows);
        } catch (Exception e) {
            log.warn("Failed to batch write car_telemetry_raw: sessionUid={}, rows={}",
                    rows.get(0).getSessionUid(), rows.size(), e);
        }
    }

    public void save(CarTelemetryRaw row) {
        try {
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to write car_telemetry_raw: sessionUid={}, frame={}, carIndex={}",
                    row.getSessionUid(), row.getFrameIdentifier(), row.getCarIndex(), e);
        }
    }

    /**
     * Builds a row without persisting (used when batching).
     */
    public CarTelemetryRaw buildRow(
            Instant ts,
            long sessionUid,
            int frameIdentifier,
            short carIndex,
            Integer speedKph,
            Float throttle,
            Float brake,
            Float sessionTimeS,
            Short lapNumber,
            Float lapDistanceM
    ) {
        Short speedKphShort = (speedKph != null && speedKph >= Short.MIN_VALUE && speedKph <= Short.MAX_VALUE)
                ? speedKph.shortValue() : null;
        return CarTelemetryRaw.builder()
                .ts(ts)
                .sessionUid(sessionUid)
                .frameIdentifier(frameIdentifier)
                .carIndex(carIndex)
                .speedKph(speedKphShort)
                .throttle(throttle)
                .brake(brake)
                .sessionTimeS(sessionTimeS)
                .lapNumber(lapNumber)
                .lapDistanceM(lapDistanceM)
                .build();
    }
}
