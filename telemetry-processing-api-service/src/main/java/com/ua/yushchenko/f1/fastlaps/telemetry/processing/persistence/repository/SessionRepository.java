package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Session write operations.
 * See: implementation_steps_plan.md § Етап 7.1.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
}
