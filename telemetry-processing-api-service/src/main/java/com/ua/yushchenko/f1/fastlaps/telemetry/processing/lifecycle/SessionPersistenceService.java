package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists session entity in its own transaction (REQUIRES_NEW) so that
 * {@link SessionLifecycleService} can serialize creation under a lock and have
 * the insert committed before the lock is released, avoiding duplicate key when
 * multiple consumers implicitly start the same session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionPersistenceService {

    private final SessionRepository sessionRepository;

    /**
     * Persist session if not already present. Runs in a new transaction so the
     * commit is visible to other threads as soon as this method returns.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSessionIfAbsent(Session session) {
        if (sessionRepository.findById(session.getSessionUid()).isEmpty()) {
            try {
                sessionRepository.save(session);
            } catch (DataIntegrityViolationException e) {
                log.debug("Session sessionUID={} already persisted by another consumer", session.getSessionUid());
            }
        }
    }
}
