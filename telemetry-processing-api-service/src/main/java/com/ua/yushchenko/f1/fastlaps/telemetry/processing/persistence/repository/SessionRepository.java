package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Session write operations.
 * Session has 1:1 session_uid (PK) and public_id (UUID). Both identify the same row.
 * Use {@link #findByPublicIdOrSessionUid(String)} for REST/WS when id can be either.
 * See: implementation_steps_plan.md § Етап 7.1.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByPublicId(UUID publicId);

    /** List sessions with non-null public_id, most recent first by creation time (for API list). */
    List<Session> findByPublicIdNotNullOrderByCreatedAtDesc(Pageable pageable);

    /** List all sessions, most recent first (for API list when public_id may be null on legacy rows). */
    List<Session> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find session by public UUID or by internal session_uid (Long).
     * Enables GET /api/sessions/{id} to work with either identifier.
     */
    default Optional<Session> findByPublicIdOrSessionUid(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return findByPublicId(UUID.fromString(trimmed));
        } catch (IllegalArgumentException e) {
            try {
                return findById(Long.parseLong(trimmed));
            } catch (NumberFormatException e2) {
                return Optional.empty();
            }
        }
    }
}
