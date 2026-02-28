package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackCornerItemDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackCornerMapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner.CornerSegment;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackCorner;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackCornerMap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackCornerMapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackCornerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates or resolves track-level corner maps. Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackCornerMapService {

    private final TrackCornerMapRepository trackCornerMapRepository;
    private final TrackCornerRepository trackCornerRepository;

    /**
     * Returns the latest corner map for track; if none exists and segments are provided, creates one (version 1).
     */
    @Transactional
    public Optional<TrackCornerMap> findOrCreateMap(Short trackId, Integer trackLengthM, List<CornerSegment> segments) {
        if (trackId == null || trackLengthM == null) {
            return Optional.empty();
        }
        Optional<TrackCornerMap> existing = trackCornerMapRepository.findLatestByTrackIdAndTrackLengthM(trackId, trackLengthM);
        if (existing.isPresent()) {
            return existing;
        }
        if (segments == null || segments.isEmpty()) {
            return Optional.empty();
        }
        TrackCornerMap map = TrackCornerMap.builder()
                .trackId(trackId)
                .trackLengthM(trackLengthM)
                .version(1)
                .algorithmParams(Map.of("algorithm", "steer-based"))
                .build();
        map = trackCornerMapRepository.save(map);
        List<TrackCorner> corners = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            CornerSegment seg = segments.get(i);
            corners.add(TrackCorner.builder()
                    .mapId(map.getId())
                    .cornerIndex((short) (i + 1))
                    .startDistanceM(seg.getStartDistanceM())
                    .endDistanceM(seg.getEndDistanceM())
                    .apexDistanceM(seg.getApexDistanceM())
                    .name("T" + (i + 1))
                    .build());
        }
        trackCornerRepository.saveAll(corners);
        log.debug("findOrCreateMap: created map id={} for trackId={}, {} corners", map.getId(), trackId, corners.size());
        return Optional.of(map);
    }

    public List<TrackCorner> getCornersByMapId(Long mapId) {
        return trackCornerRepository.findByMapIdOrderByCornerIndexAsc(mapId);
    }

    /** Returns latest corner map for track as DTO, or empty if none. */
    public Optional<TrackCornerMapResponseDto> getLatestMapDto(Short trackId, Integer trackLengthM) {
        if (trackId == null || trackLengthM == null) {
            return Optional.empty();
        }
        return trackCornerMapRepository.findLatestByTrackIdAndTrackLengthM(trackId, trackLengthM)
                .map(map -> {
                    List<TrackCorner> corners = trackCornerRepository.findByMapIdOrderByCornerIndexAsc(map.getId());
                    List<TrackCornerItemDto> items = corners.stream()
                            .map(tc -> TrackCornerItemDto.builder()
                                    .cornerIndex(tc.getCornerIndex() != null ? tc.getCornerIndex().intValue() : 0)
                                    .name(tc.getName())
                                    .startDistanceM(tc.getStartDistanceM())
                                    .endDistanceM(tc.getEndDistanceM())
                                    .apexDistanceM(tc.getApexDistanceM())
                                    .build())
                            .collect(Collectors.toList());
                    return TrackCornerMapResponseDto.builder()
                            .trackId(map.getTrackId() != null ? map.getTrackId().intValue() : null)
                            .trackLengthM(map.getTrackLengthM())
                            .version(map.getVersion())
                            .corners(items)
                            .build();
                });
    }
}
