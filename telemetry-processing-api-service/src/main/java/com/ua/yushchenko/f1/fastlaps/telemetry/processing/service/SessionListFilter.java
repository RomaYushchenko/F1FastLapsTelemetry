package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Filter and pagination parameters for GET /api/sessions list.
 * Plan: block-b-session-list-filters.md Step 7.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionListFilter {

    /** Filter by session type code (e.g. 15 for RACE). Null = all. */
    private Integer sessionType;
    /** Filter by track id. Null = all. */
    private Integer trackId;
    /** Search string: display name, session type display name, track display name (resolved in service). */
    private String search;
    /** Sort: startedAt_asc, startedAt_desc, finishingPosition_asc, bestLap_asc, bestLap_desc. Default startedAt_desc. */
    private String sort;
    /** State filter: ACTIVE, FINISHED. Null = all. */
    private String state;
    /** Date range start (inclusive). Sessions with startedAt and endedAt in [dateFrom, dateTo]; active if startedAt in range. */
    private LocalDate dateFrom;
    /** Date range end (inclusive). */
    private LocalDate dateTo;
    private int offset;
    private int limit;
}
