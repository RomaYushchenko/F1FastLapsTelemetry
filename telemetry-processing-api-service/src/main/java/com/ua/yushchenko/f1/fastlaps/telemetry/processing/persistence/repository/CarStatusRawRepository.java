package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRawId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for car_status_raw (raw car status samples).
 * See: implementation_steps_plan.md § 6.6.
 */
@Repository
public interface CarStatusRawRepository extends JpaRepository<CarStatusRaw, CarStatusRawId> {
}
