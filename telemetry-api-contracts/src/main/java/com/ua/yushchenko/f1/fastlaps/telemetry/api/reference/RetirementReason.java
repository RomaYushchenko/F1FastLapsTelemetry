package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 Event: Retirement reason (EventDataDetails.Retirement.reason).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum RetirementReason {

    INVALID(0, "Invalid"),
    RETIRED(1, "Retired"),
    FINISHED(2, "Finished"),
    TERMINAL_DAMAGE(3, "Terminal damage"),
    INACTIVE(4, "Inactive"),
    NOT_ENOUGH_LAPS(5, "Not enough laps"),
    BLACK_FLAGGED(6, "Black flagged"),
    RED_FLAGGED(7, "Red flagged"),
    MECHANICAL_FAILURE(8, "Mechanical failure"),
    SESSION_SKIPPED(9, "Session skipped"),
    SESSION_SIMULATED(10, "Session simulated"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    RetirementReason(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Resolve by F1 game code (0–10). Out-of-range returns UNKNOWN.
     */
    public static RetirementReason fromCode(int code) {
        for (RetirementReason value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static RetirementReason fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
