package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_steeringAssist (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SteeringAssist {

    OFF(0, "Off"),
    ON(1, "On"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SteeringAssist(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SteeringAssist fromCode(int code) {
        for (SteeringAssist value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SteeringAssist fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
