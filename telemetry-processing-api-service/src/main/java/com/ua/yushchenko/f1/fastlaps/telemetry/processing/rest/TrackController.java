package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.BulkImportResultDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBulkExportDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutExportDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for track-level endpoints (2D/3D layout import/export).
 * Block F — B8 layout.
 */
@Slf4j
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackLayoutService trackLayoutService;

    @GetMapping("/{trackId}/layout")
    public ResponseEntity<TrackLayoutResponseDto> getLayout(@PathVariable("trackId") Short trackId) {
        log.debug("getLayout: trackId={}", trackId);
        Optional<TrackLayoutResponseDto> dto = trackLayoutService.getLayout(trackId);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{trackId}/layout/status")
    public ResponseEntity<TrackLayoutStatusDto> getLayoutStatus(@PathVariable("trackId") short trackId) {
        log.debug("getLayoutStatus: trackId={}", trackId);
        TrackLayoutStatusDto status = trackLayoutService.getLayoutStatus(trackId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{trackId}/layout/export")
    public ResponseEntity<TrackLayoutExportDto> exportLayout(@PathVariable("trackId") Short trackId) {
        log.debug("exportLayout: trackId={}", trackId);
        return trackLayoutService.exportLayout(trackId)
                .map(dto -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"track-" + trackId + "-layout.json\"")
                        .body(dto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/layout/export-all")
    public ResponseEntity<TrackLayoutBulkExportDto> exportAllLayouts() {
        log.debug("exportAllLayouts");
        TrackLayoutBulkExportDto bulk = trackLayoutService.exportAllLayouts();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"all-tracks-layout.json\"")
                .body(bulk);
    }

    @PostMapping("/layout/import")
    public ResponseEntity<TrackLayoutResponseDto> importLayout(
            @RequestBody @Valid TrackLayoutExportDto dto
    ) {
        TrackLayoutResponseDto saved = trackLayoutService.importLayout(dto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/layout/import-all")
    public ResponseEntity<BulkImportResultDto> importAllLayouts(
            @RequestBody TrackLayoutBulkExportDto dto
    ) {
        BulkImportResultDto result = trackLayoutService.importAllLayouts(dto);
        return ResponseEntity.ok(result);
    }

}
