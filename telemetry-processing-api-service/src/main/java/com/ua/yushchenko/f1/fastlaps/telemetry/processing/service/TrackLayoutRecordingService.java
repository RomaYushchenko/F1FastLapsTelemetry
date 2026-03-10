package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.PointXYZD;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.ABORTED;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.DONE;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.IDLE;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.RECORDING;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.SAVING;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState.Status.WAITING_FOR_LAP_START;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackLayoutRecordingService {

    /** Minimum points when saving after lap complete (avoids saving empty buffer; lap completion is the only criterion). */
    private static final int MIN_POINTS_WHEN_LAP_COMPLETE = 10;

    private final TrackLayoutRepository trackLayoutRepository;
    private final SessionStateManager sessionStateManager;

    public void onSessionStart(long sessionUid, short trackId) {
        boolean exists = trackLayoutRepository.existsById(trackId);
        TrackRecordingState state = getRecState(sessionUid);
        state.setTrackId(trackId);
        state.setStatus(exists ? IDLE : WAITING_FOR_LAP_START);
        log.info("[TrackRec] trackId={} exists={} → {}", trackId, exists, state.getStatus());
    }

    /**
     * Set or correct trackId for recording from full PacketSessionData (authoritative).
     * When not yet recording (IDLE/WAITING_FOR_LAP_START): sets trackId and status.
     * When already RECORDING or SAVING: still updates trackId so that the saved layout uses the
     * correct id (SSTA can send wrong trackId on some builds; SessionData is authoritative).
     */
    public void setTrackIdFromSessionData(long sessionUid, short trackId) {
        SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);
        if (sessionState == null) {
            return;
        }
        TrackRecordingState state = sessionState.getTrackRecordingState();
        short previous = state.getTrackId();
        if (trackId < 0) {
            return;
        }
        state.setTrackId(trackId);

        TrackRecordingState.Status status = state.getStatus();
        if (status == RECORDING || status == SAVING) {
            if (previous != trackId) {
                log.info("[TrackRec] trackId corrected {} → {} from SessionData (during {}), save will use {}", previous, trackId, status, trackId);
            }
            return;
        }

        boolean exists = trackLayoutRepository.existsById(trackId);
        state.setStatus(exists ? IDLE : WAITING_FOR_LAP_START);
        if (previous == -1) {
            log.info("[TrackRec] trackId={} from SessionData (was unset), exists={} → {}", trackId, exists, state.getStatus());
        } else if (previous != trackId) {
            log.info("[TrackRec] trackId corrected {} → {} from SessionData, exists={} → {}", previous, trackId, exists, state.getStatus());
        }
    }

    /**
     * @param carIndex   car index from packet header; recording is gated to player car only
     * @param worldX     worldPositionX (horizontal)
     * @param worldY     worldPositionY (elevation)
     * @param worldZ     worldPositionZ (horizontal depth)
     * @param lapDistance lap distance from LapData (metres), used for sector boundaries
     */
    public void onMotionFrame(long sessionUid,
                              int carIndex,
                              float worldX,
                              float worldY,
                              float worldZ,
                              float lapDistance) {
        SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);
        if (sessionState == null) {
            return;
        }
        Integer playerCarIndex = sessionState.getPlayerCarIndex();
        if (playerCarIndex == null || carIndex != playerCarIndex.intValue()) {
            return;
        }

        TrackRecordingState state = sessionState.getTrackRecordingState();

        if (state.getStatus() == WAITING_FOR_LAP_START) {
            if (lapDistance > 0) {
                state.setStatus(RECORDING);
                log.info("[TrackRec] Lap started, trackId={}", state.getTrackId());
            } else if (lapDistance < 0) {
                // LapData may not be processed yet (Kafka ordering); start recording anyway to avoid losing motion
                state.setStatus(RECORDING);
                log.info("[TrackRec] Lap started (no lapDistance yet), trackId={}", state.getTrackId());
            }
        }

        if (state.getStatus() == RECORDING && state.shouldSample()) {
            state.addPoint(worldX, worldY, worldZ, lapDistance);
            int size = state.getBuffer().size();
            // Lap complete may be processed before motion (different Kafka topics); save when we have minimum points
            if (state.isPendingLapComplete() && size >= MIN_POINTS_WHEN_LAP_COMPLETE) {
                state.setPendingLapComplete(false);
                boolean invalid = state.isPendingLapInvalid();
                state.setPendingLapInvalid(false);
                state.setStatus(SAVING);
                if (invalid) {
                    log.info("[TrackRec] Saving track from deferred lap complete (lap invalid, points={})", size);
                }
                saveTrackLayout(sessionUid, state);
            }
        }
    }

    public void onLapComplete(long sessionUid, int carIndex, boolean lapInvalid) {
        SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);
        if (sessionState == null) {
            return;
        }
        Integer playerCarIndex = sessionState.getPlayerCarIndex();
        if (playerCarIndex == null || carIndex != playerCarIndex.intValue()) {
            return;
        }

        TrackRecordingState state = sessionState.getTrackRecordingState();
        if (state.getStatus() != RECORDING) {
            return;
        }

        int count = state.getBuffer().size();
        if (count < MIN_POINTS_WHEN_LAP_COMPLETE) {
            // Lap complete often arrives before motion (different Kafka topics); defer save until we have enough points
            boolean wasAlreadyPending = state.isPendingLapComplete();
            state.setPendingLapComplete(true);
            state.setPendingLapInvalid(lapInvalid);
            if (!wasAlreadyPending) {
                log.info("[TrackRec] Lap complete deferred: invalid={}, points={} (waiting for motion to reach {} pts)",
                        lapInvalid, count, MIN_POINTS_WHEN_LAP_COMPLETE);
            }
            return;
        }
        // Have enough points: save track even if lap invalid (layout geometry is still valid)
        if (lapInvalid) {
            log.info("[TrackRec] Saving track from lap complete (lap invalid, points={})", count);
        }

        state.setStatus(SAVING);
        saveTrackLayout(sessionUid, state);
    }

    public void onSessionFinished(long sessionUid) {
        SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);
        if (sessionState == null) {
            return;
        }
        TrackRecordingState state = sessionState.getTrackRecordingState();
        if (state.getStatus() == RECORDING || state.getStatus() == WAITING_FOR_LAP_START) {
            state.reset();
            state.setStatus(ABORTED);
            log.info("[TrackRec] Session finished — recording aborted");
        }
    }

    private void saveTrackLayout(long sessionUid, TrackRecordingState recState) {
        List<PointXYZD> pts = List.copyOf(recState.getBuffer());
        short trackId = recState.getTrackId();
        SessionRuntimeState sessionState = sessionStateManager.get(sessionUid);

        double minX = pts.stream().mapToDouble(PointXYZD::x).min().orElse(0);
        double maxX = pts.stream().mapToDouble(PointXYZD::x).max().orElse(0);
        double minZ = pts.stream().mapToDouble(PointXYZD::z).min().orElse(0);
        double maxZ = pts.stream().mapToDouble(PointXYZD::z).max().orElse(0);

        double minElev = pts.stream().mapToDouble(PointXYZD::y).min().orElse(0);
        double maxElev = pts.stream().mapToDouble(PointXYZD::y).max().orElse(0);

        List<Map<String, Double>> jsonPoints = pts.stream()
                .map(p -> Map.of(
                        "x", (double) p.x(),
                        "y", (double) p.y(),
                        "z", (double) p.z()
                ))
                .toList();

        List<Map<String, Object>> sectorBoundaries = buildSectorBoundaries(
                pts,
                sessionState != null ? sessionState.getSector2LapDistanceStart() : -1f,
                sessionState != null ? sessionState.getSector3LapDistanceStart() : -1f
        );

        TrackLayout entity = TrackLayout.builder()
                .trackId(trackId)
                .pointsJson(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().valueToTree(jsonPoints).toString())
                .version((short) 1)
                .minX(minX)
                .minY(minZ)
                .maxX(maxX)
                .maxY(maxZ)
                .minElev(minElev)
                .maxElev(maxElev)
                .source("RECORDED")
                .recordedAt(Instant.now())
                .sessionUid(sessionUid)
                .sectorBoundariesJson(sectorBoundaries.isEmpty()
                        ? null
                        : com.fasterxml.jackson.databind.json.JsonMapper.builder().build().valueToTree(sectorBoundaries).toString())
                .build();

        try {
            trackLayoutRepository.save(entity);
            recState.setStatus(DONE);
            log.info("[TrackRec] Saved trackId={}: {} pts, elev=[{}..{}]m",
                    trackId, pts.size(), minElev, maxElev);
        } catch (Exception e) {
            log.error("[TrackRec] Save failed for trackId={}: {}", trackId, e.getMessage(), e);
            recState.setStatus(ABORTED);
        }
    }

    private List<Map<String, Object>> buildSectorBoundaries(List<PointXYZD> pts, float s2Dist, float s3Dist) {
        if (pts.isEmpty()) {
            return List.of();
        }

        PointXYZD s1 = pts.get(0);

        PointXYZD s2 = pts.stream()
                .min(Comparator.comparingDouble(p -> Math.abs(p.lapDistance() - s2Dist)))
                .orElse(pts.get(pts.size() / 2));

        PointXYZD s3 = pts.stream()
                .min(Comparator.comparingDouble(p -> Math.abs(p.lapDistance() - s3Dist)))
                .orElse(pts.get(pts.size() * 2 / 3));

        return List.of(
                Map.of("sector", 1, "x", (double) s1.x(), "y", (double) s1.y(), "z", (double) s1.z()),
                Map.of("sector", 2, "x", (double) s2.x(), "y", (double) s2.y(), "z", (double) s2.z()),
                Map.of("sector", 3, "x", (double) s3.x(), "y", (double) s3.y(), "z", (double) s3.z())
        );
    }

    private TrackRecordingState getRecState(long sessionUid) {
        return sessionStateManager.get(sessionUid).getTrackRecordingState();
    }
}

