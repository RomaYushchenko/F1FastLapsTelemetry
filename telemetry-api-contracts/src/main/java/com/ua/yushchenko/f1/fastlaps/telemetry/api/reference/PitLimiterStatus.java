package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 CarStatusData: m_pitLimiterStatus (0 = off, 1 = on).
 * See: .github/draft/other-enume-types-deep-research-report.md, plan 11.
 */
@Getter
public enum PitLimiterStatus {

    OFF(0, "Off"),
    ON(1, "On"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    PitLimiterStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PitLimiterStatus fromCode(int code) {
        for (PitLimiterStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static PitLimiterStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
