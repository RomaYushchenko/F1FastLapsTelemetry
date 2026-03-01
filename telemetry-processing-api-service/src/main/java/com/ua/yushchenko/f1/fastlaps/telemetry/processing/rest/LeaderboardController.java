package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.CarPositionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.LeaderboardQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for live leaderboard (active session only).
 * Block E — Live leaderboard.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/active")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardQueryService leaderboardQueryService;

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard() {
        log.debug("Get leaderboard");
        List<LeaderboardEntryDto> entries = leaderboardQueryService.getLeaderboardForActiveSession();
        if (entries.isEmpty()) {
            log.debug("Get leaderboard: no active session, returning 204");
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/positions")
    public ResponseEntity<List<CarPositionDto>> getPositions() {
        log.debug("Get positions");
        List<CarPositionDto> positions = leaderboardQueryService.getPositionsForActiveSession();
        if (positions.isEmpty()) {
            log.debug("Get positions: no active session or no position data, returning 204");
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(positions);
    }
}
