package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of listSessions with paginated list and total count for X-Total-Count header.
 * Plan: block-b-session-list-filters.md Step 7.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResult {

    private List<SessionDto> list;
    private long total;
}
