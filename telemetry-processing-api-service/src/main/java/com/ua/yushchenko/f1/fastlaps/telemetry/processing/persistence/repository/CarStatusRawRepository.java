package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRawId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for car_status_raw (raw car status samples).
 * See: implementation_steps_plan.md § 6.6; 04-session-summary-page.md Etap 5 (ERS).
 */
@Repository
public interface CarStatusRawRepository extends JpaRepository<CarStatusRaw, CarStatusRawId> {

    /** Find status samples in time range for a session+car (for ERS merge by lap). */
    List<CarStatusRaw> findBySessionUidAndCarIndexAndTsBetweenOrderByTsAsc(
            Long sessionUid, Short carIndex, Instant tsStart, Instant tsEnd);
}
