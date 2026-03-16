package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.GlobalDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.DiagnosticsService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.GlobalDiagnosticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for diagnostics endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final DiagnosticsService diagnosticsService;
    private final GlobalDiagnosticsService globalDiagnosticsService;

    /**
     * Returns diagnostics for a single session, including packet health.
     */
    @GetMapping("/sessions/{publicId}/diagnostics")
    public ResponseEntity<SessionDiagnosticsDto> getSessionDiagnostics(@PathVariable("publicId") String publicId) {
        log.debug("getSessionDiagnostics: sessionPublicId={}", publicId);
        SessionDiagnosticsDto dto = diagnosticsService.getSessionDiagnostics(publicId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Returns global diagnostics snapshot, including aggregated packet health.
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<GlobalDiagnosticsDto> getGlobalDiagnostics() {
        log.debug("getGlobalDiagnostics");
        GlobalDiagnosticsDto dto = globalDiagnosticsService.getGlobalDiagnostics();
        return ResponseEntity.ok(dto);
    }
}

