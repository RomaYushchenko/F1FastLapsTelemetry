package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 CarStatusData: m_tractionControl (0 = off, 1 = medium, 2 = full).
 * See: .github/draft/other-enume-types-deep-research-report.md, plan 11.
 */
@Getter
public enum TractionControl {

    OFF(0, "Off"),
    MEDIUM(1, "Medium"),
    FULL(2, "Full"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    TractionControl(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static TractionControl fromCode(int code) {
        for (TractionControl value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static TractionControl fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
