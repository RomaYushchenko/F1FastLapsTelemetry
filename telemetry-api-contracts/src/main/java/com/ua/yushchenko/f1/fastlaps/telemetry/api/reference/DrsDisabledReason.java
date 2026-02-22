package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 Event: DRS disabled reason (EventDataDetails.DRSDisabled.reason).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum DrsDisabledReason {

    WET_TRACK(0, "Wet track"),
    SAFETY_CAR(1, "Safety car"),
    RED_FLAG(2, "Red flag"),
    MIN_LAP_NOT_REACHED(3, "Min lap not reached"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    DrsDisabledReason(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Resolve by F1 game code (0–3). Out-of-range returns UNKNOWN.
     */
    public static DrsDisabledReason fromCode(int code) {
        for (DrsDisabledReason value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DrsDisabledReason fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
