package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
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
    private final LapAggregator lapAggregator;
    private final LiveDataBroadcaster liveDataBroadcaster;

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

            // Persist session to DB
            Session session = Session.builder()
                    .sessionUid(sessionUID)
                    .packetFormat((short) 1) // F1 2024/2025 format
                    .gameMajorVersion((short) 1)
                    .gameMinorVersion((short) 0)
                    .sessionType(null) // Will be filled from session data packet
                    .trackId(event.getTrackId() != null ? event.getTrackId().shortValue() : null)
                    .totalLaps(event.getTotalLaps() != null ? event.getTotalLaps().shortValue() : null)
                    .startedAt(state.getStartedAt())
                    .build();
            sessionRepository.save(session);
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
     * Handle session timeout (no data received for timeout period).
     * Called by NoDataTimeoutWorker scheduler.
     */
    public void onSessionTimeout(long sessionUID) {
        log.warn("Session timeout detected: sessionUID={}", sessionUID);

        // Simulate SEND event with NO_DATA_TIMEOUT reason
        onSessionEnded(sessionUID, null, EndReason.NO_DATA_TIMEOUT);
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
