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

    private static final int MIN_POINTS_THRESHOLD = 300;

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
     * @param worldX     worldPositionX (horizontal)
     * @param worldY     worldPositionY (elevation)
     * @param worldZ     worldPositionZ (horizontal depth)
     * @param lapDistance lap distance from LapData (metres), used for sector boundaries
     */
    public void onMotionFrame(long sessionUid,
                              float worldX,
                              float worldY,
                              float worldZ,
                              float lapDistance) {
        TrackRecordingState state = getRecState(sessionUid);

        if (state.getStatus() == WAITING_FOR_LAP_START && lapDistance > 0) {
            state.setStatus(RECORDING);
            log.info("[TrackRec] Lap started, trackId={}", state.getTrackId());
        }

        if (state.getStatus() == RECORDING && state.shouldSample()) {
            state.addPoint(worldX, worldY, worldZ, lapDistance);
        }
    }

    public void onLapComplete(long sessionUid, boolean lapInvalid) {
        TrackRecordingState state = getRecState(sessionUid);
        if (state.getStatus() != RECORDING) {
            return;
        }

        int count = state.getBuffer().size();
        if (lapInvalid || count < MIN_POINTS_THRESHOLD) {
            log.warn("[TrackRec] Lap discarded: invalid={}, points={}", lapInvalid, count);
            state.reset();
            state.setStatus(WAITING_FOR_LAP_START);
            return;
        }

        state.setStatus(SAVING);
        saveTrackLayout(sessionUid, state);
    }

    public void onSessionFinished(long sessionUid) {
        TrackRecordingState state = getRecState(sessionUid);
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

