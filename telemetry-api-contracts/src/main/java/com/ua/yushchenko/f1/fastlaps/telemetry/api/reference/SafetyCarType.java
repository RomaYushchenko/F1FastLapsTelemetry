package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 Event: Safety car type (EventDataDetails.SafetyCar.safetyCarType).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SafetyCarType {

    NONE(0, "No Safety Car"),
    FULL(1, "Full Safety Car"),
    VIRTUAL(2, "Virtual Safety Car"),
    FORMATION_LAP(3, "Formation Lap Safety Car"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SafetyCarType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Resolve by F1 game code (0–3). Out-of-range returns UNKNOWN.
     */
    public static SafetyCarType fromCode(int code) {
        for (SafetyCarType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SafetyCarType fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
