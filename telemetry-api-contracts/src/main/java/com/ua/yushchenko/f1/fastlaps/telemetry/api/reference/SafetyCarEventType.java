package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 Event: Safety car event type (EventDataDetails.SafetyCar.eventType).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SafetyCarEventType {

    DEPLOYED(0, "Deployed"),
    RETURNING(1, "Returning"),
    RETURNED(2, "Returned"),
    RESUME_RACE(3, "Resume Race"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SafetyCarEventType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Resolve by F1 game code (0–3). Out-of-range returns UNKNOWN.
     */
    public static SafetyCarEventType fromCode(int code) {
        for (SafetyCarEventType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SafetyCarEventType fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
