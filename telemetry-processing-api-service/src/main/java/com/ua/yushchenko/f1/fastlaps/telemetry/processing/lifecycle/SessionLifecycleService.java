package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1Track;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation.LapAggregator;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.TyreCompoundMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.EndReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.LastTyreCompoundState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.LiveDataBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
    private final SessionFinishingPositionRepository finishingPositionRepository;
    private final SessionDriverRepository sessionDriverRepository;
    private final SessionPersistenceService sessionPersistenceService;
    private final LapAggregator lapAggregator;
    private final LiveDataBroadcaster liveDataBroadcaster;
    private final com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutRecordingService trackLayoutRecordingService;
    private final LastTyreCompoundState lastTyreCompoundState;

    /** Serializes session persist so only one consumer inserts; others see the row after commit. */
    private final Object sessionCreationLock = new Object();

    private static final DateTimeFormatter DISPLAY_NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);
    private static final int SESSION_DISPLAY_NAME_MAX_LENGTH = 64;
    /** session_drivers.driver_label max length. */
    private static final int DRIVER_LABEL_MAX_LENGTH = 16;

    /**
     * Build session display name from track and start time.
     * Returns null if track is unknown so caller keeps default (UUID).
     */
    private static String computeSessionDisplayName(Short trackId, Instant startedAt) {
        if (trackId == null) {
            return null;
        }
        F1Track track = F1Track.fromId(trackId.intValue());
        if (track == F1Track.UNKNOWN) {
            return null;
        }
        String trackName = track.getDisplayName();
        String timePart = startedAt == null ? "" : " – " + DISPLAY_NAME_TIME.format(startedAt);
        String name = trackName + timePart;
        return name.length() > SESSION_DISPLAY_NAME_MAX_LENGTH
                ? name.substring(0, SESSION_DISPLAY_NAME_MAX_LENGTH)
                : name;
    }

    /**
     * If session display name is still the default (UUID), set it to "TrackName – startedAt"
     * when track is known. Preserves user-edited names (PATCH). Returns true if display name was updated.
     */
    private boolean updateDisplayNameWhenStillDefault(Session session) {
        if (session.getPublicId() == null || session.getSessionDisplayName() == null) {
            return false;
        }
        if (!session.getSessionDisplayName().equals(session.getPublicId().toString())) {
            return false;
        }
        String computed = computeSessionDisplayName(session.getTrackId(), session.getStartedAt());
        if (computed == null) {
            return false;
        }
        session.setSessionDisplayName(computed);
        log.debug("Updated session display name: sessionUID={}, displayName={}", session.getSessionUid(), computed);
        return true;
    }

    /**
     * Handle session started event (SSTA).
     * Transition: INIT → ACTIVE.
     */
    public void onSessionStarted(long sessionUID, SessionEventDto event) {
        SessionRuntimeState state = stateManager.getOrCreate(sessionUID);

        if (state.getState() == SessionState.INIT) {
            log.info("Session started: sessionUID={}, sessionType={}, trackId={}",
                    sessionUID, F1SessionType.fromCode(event.getSessionTypeId()).getDisplayName(), event.getTrackId());

            state.setStartedAt(Instant.now());
            state.transitionTo(SessionState.ACTIVE);

            Short trackIdShort = event.getTrackId() != null ? event.getTrackId().shortValue() : null;
            String displayName = computeSessionDisplayName(trackIdShort, state.getStartedAt());

            Session session = Session.builder()
                    .sessionUid(sessionUID)
                    .packetFormat((short) 1) // F1 2024/2025 format
                    .gameMajorVersion((short) 1)
                    .gameMinorVersion((short) 0)
                    .sessionType(event.getSessionTypeId() != null ? event.getSessionTypeId().shortValue() : null)
                    .trackId(trackIdShort)
                    .totalLaps(event.getTotalLaps() != null ? event.getTotalLaps().shortValue() : null)
                    .startedAt(state.getStartedAt())
                    .sessionDisplayName(displayName)
                    .build();
            persistSessionUnderLock(session);
            if (trackIdShort != null) {
                trackLayoutRecordingService.onSessionStart(sessionUID, trackIdShort);
            }
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

            // Update session in DB (set ended_at, end_reason) and persist finishing position if available
            sessionRepository.findById(sessionUID).ifPresent(session -> {
                session.setEndedAt(state.getEndedAt());
                session.setEndReason(reason.name());
                sessionRepository.save(session);

                persistFinishingPositionsForAllCars(sessionUID, state);
                // Persist participant names from Participants packet (packetId=4) for leaderboard/session detail after restart
                persistParticipantsFromState(sessionUID, state);
            });

            // Notify WebSocket subscribers
            liveDataBroadcaster.notifySessionEnded(sessionUID, reason.name());

            // After flush, transition to TERMINAL
            finalizeSession(sessionUID);
            // Do not purge processed_packets here: Kafka may still redeliver; time-based retention removes rows.
            trackLayoutRecordingService.onSessionFinished(sessionUID);
        } else {
            log.warn("Received SEND for session in state {}, ignoring (sessionUID={})",
                    state.getState(), sessionUID);
        }
    }

    /**
     * Persist finishing position and tyre compound for every car that has a known race position in runtime state.
     */
    private void persistFinishingPositionsForAllCars(long sessionUID, SessionRuntimeState state) {
        Map<Integer, Integer> positions = state.getLastCarPositionByCarIndex();
        int maxCar = state.getNumActiveCars();
        for (Map.Entry<Integer, Integer> e : positions.entrySet()) {
            Integer pos = e.getValue();
            if (pos == null || pos <= 0) {
                continue;
            }
            int carIndex = e.getKey();
            if (carIndex < 0 || carIndex >= maxCar) {
                continue;
            }
            String tyreCompound = TyreCompoundMapper.toPersistedCompound(state.getSnapshot(carIndex));
            if (tyreCompound == null) {
                Short actual = lastTyreCompoundState.get(sessionUID, carIndex);
                tyreCompound = TyreCompoundMapper.toPersistedFromActualCompound(actual);
            }
            SessionFinishingPosition fp = SessionFinishingPosition.builder()
                    .sessionUid(sessionUID)
                    .carIndex((short) carIndex)
                    .finishingPosition(pos)
                    .tyreCompound(tyreCompound)
                    .build();
            finishingPositionRepository.save(fp);
            log.info("Saved finishing position: sessionUID={}, carIndex={}, position={}, tyreCompound={}",
                    sessionUID, carIndex, pos, tyreCompound);
        }
    }

    /**
     * Persist participant names from runtime state to session_drivers so leaderboard and session detail show driver names after restart.
     * Truncates to DRIVER_LABEL_MAX_LENGTH to fit column.
     */
    private void persistParticipantsFromState(long sessionUID, SessionRuntimeState state) {
        Instant now = Instant.now();
        int maxCar = state.getNumActiveCars();
        for (int carIndex = 0; carIndex < maxCar; carIndex++) {
            String name = state.getParticipantName(carIndex);
            if (name == null || name.isBlank()) {
                continue;
            }
            String label = name.length() > DRIVER_LABEL_MAX_LENGTH
                    ? name.substring(0, DRIVER_LABEL_MAX_LENGTH)
                    : name.trim();
            SessionDriver sd = sessionDriverRepository.findBySessionUidAndCarIndex(sessionUID, (short) carIndex)
                    .orElse(SessionDriver.builder()
                            .sessionUid(sessionUID)
                            .carIndex((short) carIndex)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
            sd.setDriverLabel(label);
            sd.setUpdatedAt(now);
            sessionDriverRepository.save(sd);
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
     * When track becomes known, updates display name from default (UUID) to "TrackName – startedAt".
     */
    public void onSessionInfo(long sessionUID, SessionEventDto event) {
        sessionRepository.findById(sessionUID).ifPresent(session -> {
            boolean updated = false;
            if (session.getSessionType() == null && event.getSessionTypeId() != null) {
                session.setSessionType(event.getSessionTypeId().shortValue());
                updated = true;
            }
            // SessionData/SessionInfo are authoritative for trackId (SSTA can send wrong id on some builds)
            if (event.getTrackId() != null && event.getTrackId().intValue() >= 0) {
                session.setTrackId(event.getTrackId().shortValue());
                updated = true;
            }
            if (session.getTotalLaps() == null && event.getTotalLaps() != null) {
                session.setTotalLaps(event.getTotalLaps().shortValue());
                updated = true;
            }
            if (updateDisplayNameWhenStillDefault(session)) {
                updated = true;
            }
            if (updated) {
                sessionRepository.save(session);
                log.info("Updated session metadata: sessionUID={}, sessionType={}, trackId={}",
                        sessionUID, session.getSessionType(), session.getTrackId());
            }
            if (event.getTrackId() != null) {
                short tid = event.getTrackId().shortValue();
                if (tid >= 0) {
                    trackLayoutRecordingService.setTrackIdFromSessionData(sessionUID, tid);
                }
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
            if (dto.getSector2LapDistanceStart() != null) {
                runtimeState.setSector2LapDistanceStart(dto.getSector2LapDistanceStart());
            }
            if (dto.getSector3LapDistanceStart() != null) {
                runtimeState.setSector3LapDistanceStart(dto.getSector3LapDistanceStart());
            }
        }
        sessionRepository.findById(sessionUID).ifPresent(session -> {
            boolean updated = false;
            // Always take session type from PacketSessionData when present (overrides SSTA/unknown)
            if (dto.getSessionType() != null) {
                session.setSessionType(dto.getSessionType().shortValue());
                updated = true;
            }
            // PacketSessionData is authoritative for trackId (SSTA can send wrong id on some builds)
            if (dto.getTrackId() != null && dto.getTrackId().intValue() >= 0) {
                session.setTrackId(dto.getTrackId().shortValue());
                updated = true;
            }
            if (dto.getTrackId() != null) {
                short tid = dto.getTrackId().shortValue();
                if (tid >= 0) {
                    trackLayoutRecordingService.setTrackIdFromSessionData(sessionUID, tid);
                }
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
            if (updateDisplayNameWhenStillDefault(session)) {
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
        SessionRuntimeState state = stateManager.get(sessionUID);
        if (state != null) {
            state.setPlayerCarIndex(carIndex);
        }
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
