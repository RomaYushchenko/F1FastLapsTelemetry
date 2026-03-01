package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1Track;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves search text to session type codes and track IDs for session list filter.
 * Search matches: (1) session display name LIKE, (2) F1SessionType display name (case-insensitive),
 * (3) F1Track display name (case-insensitive). Service combines with OR in specification.
 * Plan: block-b-session-list-filters.md Step 7.4.
 */
@Component
public class SessionSearchResolver {

    /**
     * Session type codes whose display name contains the search string (case-insensitive).
     * Empty list if search is blank.
     */
    public List<Short> resolveSessionTypeCodes(String search) {
        if (search == null || search.isBlank()) {
            return Collections.emptyList();
        }
        String lower = search.trim().toLowerCase();
        return Arrays.stream(F1SessionType.values())
                .filter(st -> st.getDisplayName().toLowerCase().contains(lower))
                .map(st -> (short) st.getCode())
                .collect(Collectors.toList());
    }

    /**
     * Track IDs whose display name contains the search string (case-insensitive).
     * Empty list if search is blank.
     */
    public List<Integer> resolveTrackIds(String search) {
        if (search == null || search.isBlank()) {
            return Collections.emptyList();
        }
        String lower = search.trim().toLowerCase();
        return Arrays.stream(F1Track.values())
                .filter(t -> t.getDisplayName().toLowerCase().contains(lower))
                .map(t -> t.getId())
                .collect(Collectors.toList());
    }
}
