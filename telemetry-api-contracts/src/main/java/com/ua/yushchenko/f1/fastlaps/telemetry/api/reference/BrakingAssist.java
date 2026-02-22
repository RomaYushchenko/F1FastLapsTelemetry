package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_brakingAssist (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum BrakingAssist {

    OFF(0, "Off"),
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    BrakingAssist(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static BrakingAssist fromCode(int code) {
        for (BrakingAssist value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static BrakingAssist fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
