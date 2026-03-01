package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBoundsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    private TrackLayoutResponseDto toDto(TrackLayout entity) {
        List<TrackLayoutPointDto> points = parsePoints(entity.getPointsJson());
        TrackLayoutBoundsDto bounds = null;
        if (entity.getMinX() != null && entity.getMinY() != null && entity.getMaxX() != null && entity.getMaxY() != null) {
            bounds = TrackLayoutBoundsDto.builder()
                    .minX(entity.getMinX())
                    .minY(entity.getMinY())
                    .maxX(entity.getMaxX())
                    .maxY(entity.getMaxY())
                    .build();
        }
        return TrackLayoutResponseDto.builder()
                .trackId(entity.getTrackId() != null ? entity.getTrackId().intValue() : null)
                .points(points)
                .bounds(bounds)
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
}
