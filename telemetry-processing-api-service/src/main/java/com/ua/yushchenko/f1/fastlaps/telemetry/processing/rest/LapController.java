package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for lap endpoints.
 * See: implementation_steps_plan.md § Етап 8.3-8.4.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionUid}")
@RequiredArgsConstructor
public class LapController {

    private final LapRepository lapRepository;

    /**
     * GET /api/sessions/{sessionUid}/laps - Get laps for session.
     */
    @GetMapping("/laps")
    public List<LapResponseDto> getLaps(
            @PathVariable("sessionUid") Long sessionUid,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("Get laps: sessionUid={}, carIndex={}", sessionUid, carIndex);
        
        return lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/sessions/{sessionUid}/sectors - Get sectors (same as laps for MVP).
     */
    @GetMapping("/sectors")
    public List<LapResponseDto> getSectors(
            @PathVariable("sessionUid") Long sessionUid,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("Get sectors: sessionUid={}, carIndex={}", sessionUid, carIndex);
        
        // For MVP, sectors are part of laps
        return getLaps(sessionUid, carIndex);
    }

    /**
     * Convert Lap entity to REST DTO.
     */
    private LapResponseDto toDto(Lap lap) {
        return LapResponseDto.builder()
                .lapNumber(lap.getLapNumber())
                .lapTimeMs(lap.getLapTimeMs())
                .sector1Ms(lap.getSector1TimeMs())
                .sector2Ms(lap.getSector2TimeMs())
                .sector3Ms(lap.getSector3TimeMs())
                .isInvalid(lap.getIsInvalid() != null && lap.getIsInvalid())
                .build();
    }
}
