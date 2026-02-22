package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketLapData: m_pitLaneTimerActive (0 = inactive, 1 = active).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum PitLaneTimerActive {

    INACTIVE(0, "Inactive"),
    ACTIVE(1, "Active"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    PitLaneTimerActive(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PitLaneTimerActive fromCode(int code) {
        for (PitLaneTimerActive value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static PitLaneTimerActive fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
