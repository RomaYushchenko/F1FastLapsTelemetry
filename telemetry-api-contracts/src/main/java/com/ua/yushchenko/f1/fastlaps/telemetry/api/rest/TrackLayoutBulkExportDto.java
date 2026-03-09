package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk export format for all track layouts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackLayoutBulkExportDto {

    private int exportVersion;
    private String exportedAt;
    private int count;
    private List<TrackLayoutExportDto> tracks;
}

