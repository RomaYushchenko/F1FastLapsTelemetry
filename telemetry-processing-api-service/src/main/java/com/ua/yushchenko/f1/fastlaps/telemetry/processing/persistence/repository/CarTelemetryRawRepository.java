package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRawId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for car_telemetry_raw (pedal trace and raw telemetry).
 * See: PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md.
 */
@Repository
public interface CarTelemetryRawRepository extends JpaRepository<CarTelemetryRaw, CarTelemetryRawId> {

    /**
     * Find trace samples for a lap, ordered by frame (distance order).
     * Used by GET /api/sessions/{id}/laps/{lapNum}/trace.
     */
    List<CarTelemetryRaw> findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
            Long sessionUid, Short carIndex, Short lapNumber);
}
