package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of bulk track layout import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResultDto {

    private int imported;
    private int skipped;
    private List<String> errors;
}

