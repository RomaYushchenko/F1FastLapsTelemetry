package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.BulkImportResultDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SectorBoundaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBoundsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBoundsExportDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBulkExportDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutExportDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Serves 2D track layout (points + optional bounds) for Live Track Map. Block F — B8.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackLayoutService {

    private final TrackLayoutRepository trackLayoutRepository;
    private final ObjectMapper objectMapper;

    /**
     * Returns layout for the given track, or empty if not found or trackId is null.
     */
    public Optional<TrackLayoutResponseDto> getLayout(Short trackId) {
        log.debug("getLayout: trackId={}", trackId);
        if (trackId == null) {
            log.warn("getLayout: trackId is null");
            return Optional.empty();
        }
        return trackLayoutRepository.findById(trackId)
                .map(this::toDto);
    }

    /**
     * Returns recording/availability status for track layout.
     */
    public TrackLayoutStatusDto getLayoutStatus(short trackId) {
        log.debug("getLayoutStatus: trackId={}", trackId);
        return trackLayoutRepository.findById(trackId)
                .map(entity -> {
                    int pointsCount = 0;
                    if (entity.getPointsJson() != null && !entity.getPointsJson().isBlank()) {
                        try {
                            List<?> pts = objectMapper.readValue(entity.getPointsJson(), new TypeReference<List<?>>() {});
                            pointsCount = pts.size();
                        } catch (Exception e) {
                            log.warn("getLayoutStatus: failed to parse points JSON for trackId={}: {}", trackId, e.getMessage());
                        }
                    }
                    String source = entity.getSource();
                    return new TrackLayoutStatusDto(
                            trackId,
                            "READY",
                            pointsCount,
                            source
                    );
                })
                .orElseGet(() -> new TrackLayoutStatusDto(trackId, "NOT_AVAILABLE", 0, null));
    }

    /**
     * Exports single track layout for given trackId.
     */
    public Optional<TrackLayoutExportDto> exportLayout(Short trackId) {
        log.debug("exportLayout: trackId={}", trackId);
        if (trackId == null) {
            log.warn("exportLayout: trackId is null");
            return Optional.empty();
        }
        return trackLayoutRepository.findById(trackId)
                .map(this::toExportDto);
    }

    /**
     * Exports all available track layouts in one bundle.
     */
    public TrackLayoutBulkExportDto exportAllLayouts() {
        log.debug("exportAllLayouts");
        List<TrackLayout> entities = trackLayoutRepository.findAll();
        List<TrackLayoutExportDto> tracks = entities.stream()
                .map(this::toExportDto)
                .sorted(Comparator.comparingInt(TrackLayoutExportDto::getTrackId))
                .toList();
        log.info("exportAllLayouts: exporting {} tracks", tracks.size());
        return TrackLayoutBulkExportDto.builder()
                .exportVersion(1)
                .exportedAt(Instant.now().toString())
                .count(tracks.size())
                .tracks(tracks)
                .build();
    }

    /**
     * Imports or updates a single track layout (upsert by trackId).
     */
    public TrackLayoutResponseDto importLayout(TrackLayoutExportDto dto) {
        log.info("importLayout: trackId={}, source={}, points={}",
                dto.getTrackId(), dto.getSource(), dto.getPoints() != null ? dto.getPoints().size() : 0);
        if (dto.getTrackId() <= 0) {
            throw new IllegalArgumentException("trackId must be positive");
        }
        if (dto.getPoints() == null || dto.getPoints().isEmpty()) {
            throw new IllegalArgumentException("points must not be empty");
        }

        TrackLayoutBoundsExportDto boundsDto = dto.getBounds();

        TrackLayout entity = TrackLayout.builder()
                .trackId((short) dto.getTrackId())
                .pointsJson(serializePoints(dto.getPoints()))
                .version((short) (dto.getVersion() > 0 ? dto.getVersion() : 1))
                .minX(boundsDto != null ? boundsDto.getMinX() : null)
                .minY(boundsDto != null ? boundsDto.getMinZ() : null)
                .maxX(boundsDto != null ? boundsDto.getMaxX() : null)
                .maxY(boundsDto != null ? boundsDto.getMaxZ() : null)
                .minElev(boundsDto != null ? boundsDto.getMinElev() : null)
                .maxElev(boundsDto != null ? boundsDto.getMaxElev() : null)
                .sectorBoundariesJson(serializeSectorBoundaries(dto.getSectorBoundaries()))
                .source(dto.getSource() != null ? dto.getSource() : "IMPORTED")
                .recordedAt(null)
                .sessionUid(null)
                .build();

        TrackLayout saved = trackLayoutRepository.save(entity);
        log.info("importLayout: saved trackId={} ({} points)", saved.getTrackId(),
                dto.getPoints().size());
        return toDto(saved);
    }

    /**
     * Bulk import of multiple track layouts. One invalid track does not stop others.
     */
    public BulkImportResultDto importAllLayouts(TrackLayoutBulkExportDto dto) {
        log.info("importAllLayouts: {} tracks", dto.getTracks() != null ? dto.getTracks().size() : 0);
        if (dto.getTracks() == null || dto.getTracks().isEmpty()) {
            return BulkImportResultDto.builder()
                    .imported(0)
                    .skipped(0)
                    .errors(List.of())
                    .build();
        }

        int imported = 0;
        List<String> errors = new ArrayList<>();

        for (TrackLayoutExportDto track : dto.getTracks()) {
            try {
                if (track.getTrackId() <= 0 || track.getPoints() == null || track.getPoints().isEmpty()) {
                    errors.add("Track " + track.getTrackId() + ": invalid data (no points)");
                    continue;
                }
                importLayout(track);
                imported++;
            } catch (Exception e) {
                log.warn("importAllLayouts: failed for trackId={}: {}", track.getTrackId(), e.getMessage());
                errors.add("Track " + track.getTrackId() + ": " + e.getMessage());
            }
        }

        log.info("importAllLayouts: imported={}, errors={}", imported, errors.size());
        return BulkImportResultDto.builder()
                .imported(imported)
                .skipped(errors.size())
                .errors(errors)
                .build();
    }

    private TrackLayoutResponseDto toDto(TrackLayout entity) {
        List<TrackLayoutPointDto> points = parsePoints(entity.getPointsJson());
        TrackLayoutBoundsDto bounds = null;
        if (entity.getMinX() != null
                && entity.getMinY() != null
                && entity.getMaxX() != null
                && entity.getMaxY() != null) {
            bounds = TrackLayoutBoundsDto.builder()
                    .minX(entity.getMinX())
                    .maxX(entity.getMaxX())
                    .minZ(entity.getMinY())
                    .maxZ(entity.getMaxY())
                    .minElev(entity.getMinElev())
                    .maxElev(entity.getMaxElev())
                    .build();
        }
        List<SectorBoundaryDto> sectorBoundaries = parseSectorBoundaries(entity.getSectorBoundariesJson());
        return TrackLayoutResponseDto.builder()
                .trackId(entity.getTrackId() != null ? entity.getTrackId().intValue() : null)
                .points(points)
                .bounds(bounds)
                .source(entity.getSource())
                .sectorBoundaries(sectorBoundaries)
                .build();
    }

    private TrackLayoutExportDto toExportDto(TrackLayout entity) {
        List<TrackLayoutPointDto> points = parsePoints(entity.getPointsJson());
        TrackLayoutBoundsExportDto bounds = null;
        if (entity.getMinX() != null
                && entity.getMinY() != null
                && entity.getMaxX() != null
                && entity.getMaxY() != null
        ) {
            bounds = TrackLayoutBoundsExportDto.builder()
                    .minX(entity.getMinX())
                    .maxX(entity.getMaxX())
                    .minZ(entity.getMinY())
                    .maxZ(entity.getMaxY())
                    .minElev(entity.getMinElev() != null ? entity.getMinElev() : 0.0)
                    .maxElev(entity.getMaxElev() != null ? entity.getMaxElev() : 0.0)
                    .build();
        }
        return TrackLayoutExportDto.builder()
                .exportVersion(1)
                .exportedAt(Instant.now().toString())
                .trackId(entity.getTrackId() != null ? entity.getTrackId() : 0)
                .trackName(null)
                .version(entity.getVersion() != null ? entity.getVersion() : 1)
                .source(entity.getSource())
                .points(points)
                .bounds(bounds)
                .sectorBoundaries(parseSectorBoundaries(entity.getSectorBoundariesJson()))
                .build();
    }

    private List<TrackLayoutPointDto> parsePoints(String pointsJson) {
        if (pointsJson == null || pointsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(pointsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("getLayout: failed to parse points JSON for track, returning empty list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SectorBoundaryDto> parseSectorBoundaries(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("getLayout: failed to parse sector boundaries JSON for track, returning empty list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializePoints(List<TrackLayoutPointDto> points) {
        try {
            return objectMapper.writeValueAsString(points);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize track layout points", e);
        }
    }

    private String serializeSectorBoundaries(List<SectorBoundaryDto> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(boundaries);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize sector boundaries", e);
        }
    }
}
