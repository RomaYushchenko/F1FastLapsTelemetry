package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation.LapAggregator;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.EndReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.LiveDataBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Session lifecycle and FSM transitions.
 * Handles SSTA (session start), SEND (session end), and state transitions.
 * See: implementation_steps_plan.md § Етап 4.6–4.8.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionLifecycleService {

    private final SessionStateManager stateManager;
    private final SessionRepository sessionRepository;
    private final SessionPersistenceService sessionPersistenceService;
    private final LapAggregator lapAggregator;
    private final LiveDataBroadcaster liveDataBroadcaster;

    /** Serializes session persist so only one consumer inserts; others see the row after commit. */
    private final Object sessionCreationLock = new Object();

    /**
     * Handle session started event (SSTA).
     * Transition: INIT → ACTIVE.
     */
    public void onSessionStarted(long sessionUID, SessionEventDto event) {
        SessionRuntimeState state = stateManager.getOrCreate(sessionUID);

        if (state.getState() == SessionState.INIT) {
            log.info("Session started: sessionUID={}, sessionType={}, trackId={}",
                    sessionUID, event.getSessionType(), event.getTrackId());

            state.setStartedAt(Instant.now());
            state.transitionTo(SessionState.ACTIVE);

            Session session = Session.builder()
                    .sessionUid(sessionUID)
                    .packetFormat((short) 1) // F1 2024/2025 format
                    .gameMajorVersion((short) 1)
                    .gameMinorVersion((short) 0)
                    .sessionType(event.getSessionTypeId() != null ? event.getSessionTypeId().shortValue() : null)
                    .trackId(event.getTrackId() != null ? event.getTrackId().shortValue() : null)
                    .totalLaps(event.getTotalLaps() != null ? event.getTotalLaps().shortValue() : null)
                    .startedAt(state.getStartedAt())
                    .build();
            persistSessionUnderLock(session);
        } else {
            log.warn("Received SSTA for session in state {}, ignoring (sessionUID={})",
                    state.getState(), sessionUID);
        }
    }

    /**
     * Handle session ended event (SEND).
     * Transition: ACTIVE → ENDING.
     */
    public void onSessionEnded(long sessionUID, SessionEventDto event, EndReason reason) {
        SessionRuntimeState state = stateManager.get(sessionUID);
        if (state == null) {
            log.warn("Received SEND for unknown session sessionUID={}", sessionUID);
            return;
        }

        if (state.getState() == SessionState.ACTIVE) {
            log.info("Session ending: sessionUID={}, reason={}", sessionUID, reason);

            state.setEndedAt(Instant.now());
            state.setEndReason(reason);
            state.transitionTo(SessionState.ENDING);

            // Flush pending aggregations (finalize laps, session summary)
            lapAggregator.finalizeAllLaps(sessionUID);

            // Update session in DB (set ended_at, end_reason)
            sessionRepository.findById(sessionUID).ifPresent(session -> {
                session.setEndedAt(state.getEndedAt());
                session.setEndReason(reason.name());
                sessionRepository.save(session);
            });

            // Notify WebSocket subscribers
            liveDataBroadcaster.notifySessionEnded(sessionUID, reason.name());

            // After flush, transition to TERMINAL
            finalizeSession(sessionUID);
        } else {
            log.warn("Received SEND for session in state {}, ignoring (sessionUID={})",
                    state.getState(), sessionUID);
        }
    }

    /**
     * Finalize session after all data is flushed.
     * Transition: ENDING → TERMINAL.
     */
    private void finalizeSession(long sessionUID) {
        SessionRuntimeState state = stateManager.get(sessionUID);
        if (state != null && state.getState() == SessionState.ENDING) {
            log.info("Finalizing session sessionUID={}", sessionUID);
            stateManager.close(sessionUID); // Transitions to TERMINAL
        }
    }

    /**
     * Handle session metadata (SESSION_INFO).
     * Updates session type and track when session was created without SSTA (e.g. implicit start).
     */
    public void onSessionInfo(long sessionUID, SessionEventDto event) {
        sessionRepository.findById(sessionUID).ifPresent(session -> {
            boolean updated = false;
            if (session.getSessionType() == null && event.getSessionTypeId() != null) {
                session.setSessionType(event.getSessionTypeId().shortValue());
                updated = true;
            }
            if (session.getTrackId() == null && event.getTrackId() != null) {
                session.setTrackId(event.getTrackId().shortValue());
                updated = true;
            }
            if (session.getTotalLaps() == null && event.getTotalLaps() != null) {
                session.setTotalLaps(event.getTotalLaps().shortValue());
                updated = true;
            }
            if (updated) {
                sessionRepository.save(session);
                log.info("Updated session metadata: sessionUID={}, sessionType={}, trackId={}",
                        sessionUID, session.getSessionType(), session.getTrackId());
            }
        });
    }

    /**
     * Handle session timeout (no data received for timeout period).
     * Called by NoDataTimeoutWorker scheduler.
     */
    public void onSessionTimeout(long sessionUID) {
        log.warn("Session timeout detected: sessionUID={}", sessionUID);

        // Simulate SEND event with NO_DATA_TIMEOUT reason
        onSessionEnded(sessionUID, null, EndReason.NO_DATA_TIMEOUT);
    }

    /**
     * Handle full session data (PacketSessionData, 724 bytes).
     * Ensures session exists and updates metadata (session type, track, laps).
     * Used when the game sends full session packet at start of practice/qualifying/race before SSTA or lap data.
     */
    public void onSessionData(long sessionUID, SessionDataDto dto) {
        if (dto == null) {
            return;
        }
        ensureSessionActive(sessionUID);
        // Keep session alive for no-data timeout: PacketSessionData counts as activity (safety car / pit don't suspend)
        SessionRuntimeState runtimeState = stateManager.get(sessionUID);
        if (runtimeState != null) {
            runtimeState.setLastSeenAt(Instant.now());
        }
        sessionRepository.findById(sessionUID).ifPresent(session -> {
            boolean updated = false;
            // Always take session type from PacketSessionData when present (overrides SSTA/unknown)
            if (dto.getSessionType() != null) {
                session.setSessionType(dto.getSessionType().shortValue());
                updated = true;
            }
            if (session.getTrackId() == null && dto.getTrackId() != null) {
                session.setTrackId(dto.getTrackId().shortValue());
                updated = true;
            }
            if (session.getTotalLaps() == null && dto.getTotalLaps() != null) {
                session.setTotalLaps(dto.getTotalLaps().shortValue());
                updated = true;
            }
            if (session.getTrackLengthM() == null && dto.getTrackLength() != null) {
                session.setTrackLengthM(dto.getTrackLength());
                updated = true;
            }
            if (session.getAiDifficulty() == null && dto.getAiDifficulty() != null) {
                session.setAiDifficulty(dto.getAiDifficulty().shortValue());
                updated = true;
            }
            if (updated) {
                sessionRepository.save(session);
                log.info("Updated session from SessionData: sessionUID={}, sessionType={}, trackId={}",
                        sessionUID, session.getSessionType(), session.getTrackId());
            }
        });
    }

    /**
     * Ensure session has runtime state and is ACTIVE.
     * If no SSTA was received, create state and transition to ACTIVE on first data packet (implicit session start).
     * This allows processing when the game does not send session events or telemetry.session is empty.
     * Persist is serialized per session so only one consumer inserts; others see the row after commit.
     */
    public void ensureSessionActive(long sessionUID) {
        SessionRuntimeState state = stateManager.getOrCreate(sessionUID);
        if (state.getState() == SessionState.INIT) {
            log.info("Implicit session start: sessionUID={} (no SSTA received, starting on first data packet)", sessionUID);
            state.setStartedAt(Instant.now());
            state.transitionTo(SessionState.ACTIVE);
            Session session = Session.builder()
                    .sessionUid(sessionUID)
                    .packetFormat((short) 1)
                    .gameMajorVersion((short) 1)
                    .gameMinorVersion((short) 0)
                    .sessionType(null)
                    .trackId(null)
                    .totalLaps(null)
                    .startedAt(state.getStartedAt())
                    .build();
            persistSessionUnderLock(session);
        }
    }

    private void persistSessionUnderLock(Session session) {
        synchronized (sessionCreationLock) {
            sessionPersistenceService.persistSessionIfAbsent(session);
        }
    }

    /**
     * Update player car index for the session from incoming telemetry.
     * Ingest sends only the player car; the game puts current player index in every packet header.
     * We always persist the latest value so that if the index ever changes mid-session (e.g. car switch),
     * the UI keeps showing data for the current player. First set is logged at INFO; changes at DEBUG.
     */
    public void setPlayerCarIndex(long sessionUID, short carIndex) {
        sessionRepository.findById(sessionUID).ifPresent(session -> {
            Short current = session.getPlayerCarIndex();
            if (current == null) {
                session.setPlayerCarIndex(carIndex);
                sessionRepository.save(session);
                log.info("Set player car index: sessionUID={}, playerCarIndex={}", sessionUID, carIndex);
            } else if (current.shortValue() != carIndex) {
                session.setPlayerCarIndex(carIndex);
                sessionRepository.save(session);
                log.debug("Player car index changed: sessionUID={}, previous={}, current={}", sessionUID, current, carIndex);
            }
        });
    }

    /**
     * Check if session should process packets (not before SSTA, not after TERMINAL).
     */
    public boolean shouldProcessPacket(long sessionUID) {
        SessionRuntimeState state = stateManager.get(sessionUID);
        if (state == null) {
            // Session not yet created (before SSTA) - should ignore packets
            log.debug("Packet before SSTA ignored: sessionUID={}", sessionUID);
            return false;
        }

        if (state.isTerminal()) {
            log.debug("Late packet after session end ignored: sessionUID={}", sessionUID);
            return false;
        }

        return true;
    }
}
