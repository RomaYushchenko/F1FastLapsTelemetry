package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 session type (m_sessionType, uint8).
 * Single source of truth for code → display string. See: .github/docs/session_type_mapping.md,
 * .github/draft/type-name-track-deep-research-report.md.
 */
@Getter
public enum F1SessionType {

    UNKNOWN(0, "UNKNOWN"),
    PRACTICE_1(1, "PRACTICE_1"),
    PRACTICE_2(2, "PRACTICE_2"),
    PRACTICE_3(3, "PRACTICE_3"),
    SHORT_PRACTICE(4, "SHORT_PRACTICE"),
    QUALIFYING_1(5, "QUALIFYING_1"),
    QUALIFYING_2(6, "QUALIFYING_2"),
    QUALIFYING_3(7, "QUALIFYING_3"),
    SHORT_QUALIFYING(8, "SHORT_QUALIFYING"),
    ONE_SHOT_QUALIFYING(9, "ONE_SHOT_QUALIFYING"),
    SPRINT_SHOOTOUT_1(10, "SPRINT_SHOOTOUT_1"),
    SPRINT_SHOOTOUT_2(11, "SPRINT_SHOOTOUT_2"),
    SPRINT_SHOOTOUT_3(12, "SPRINT_SHOOTOUT_3"),
    SHORT_SPRINT_SHOOTOUT(13, "SHORT_SPRINT_SHOOTOUT"),
    ONE_SHOT_SPRINT_SHOOTOUT(14, "ONE_SHOT_SPRINT_SHOOTOUT"),
    RACE(15, "RACE"),
    RACE_2(16, "RACE_2"),
    RACE_3(17, "RACE_3"),
    TIME_TRIAL(18, "TIME_TRIAL");

    private final int code;
    private final String displayName;

    F1SessionType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Resolve session type by F1 game code (0–18). Unknown or out-of-range returns UNKNOWN.
     */
    public static F1SessionType fromCode(int code) {
        int normalized = code & 0xFF;
        for (F1SessionType value : values()) {
            if (value.code == normalized) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Null-safe: null → UNKNOWN.
     */
    public static F1SessionType fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
