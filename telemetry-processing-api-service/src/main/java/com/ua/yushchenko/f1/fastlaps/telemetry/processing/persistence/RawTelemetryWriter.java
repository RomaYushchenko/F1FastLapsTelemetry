package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
     * Persist one telemetry sample. Called from CarTelemetryConsumer when session is ACTIVE.
     *
     * @param ts             timestamp for the row (e.g. Instant.now())
     * @param sessionUid     session id
     * @param frameIdentifier frame id
     * @param carIndex      car index
     * @param speedKph      speed (optional)
     * @param throttle      throttle 0-1 (optional)
     * @param brake         brake 0-1 (optional)
     * @param sessionTimeS  session time in seconds (optional)
     * @param lapNumber     current lap number from LapData state (optional)
     * @param lapDistanceM  lap distance in metres from LapData state (optional)
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
        try {
            Short speedKphShort = (speedKph != null && speedKph >= Short.MIN_VALUE && speedKph <= Short.MAX_VALUE)
                    ? speedKph.shortValue() : null;
            CarTelemetryRaw row = CarTelemetryRaw.builder()
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
            repository.save(row);
        } catch (Exception e) {
            log.warn("Failed to write car_telemetry_raw: sessionUid={}, frame={}, carIndex={}", 
                    sessionUid, frameIdentifier, carIndex, e);
        }
    }
}
