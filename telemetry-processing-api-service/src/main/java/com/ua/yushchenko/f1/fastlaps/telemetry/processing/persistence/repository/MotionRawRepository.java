package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.MotionRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.MotionRawId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for motion_raw. Used to join with car_telemetry_raw by (session_uid, frame_identifier, car_index).
 */
@Repository
public interface MotionRawRepository extends JpaRepository<MotionRaw, MotionRawId> {

    /**
     * Find motion samples for session/car in frame range (for one lap). Order by frame for merge with telemetry.
     */
    List<MotionRaw> findBySessionUidAndCarIndexAndFrameIdentifierBetweenOrderByFrameIdentifierAsc(
            Long sessionUid, Short carIndex, Integer frameMin, Integer frameMax);
}
