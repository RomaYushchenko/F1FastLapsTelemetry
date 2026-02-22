package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_gearboxAssist (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum GearboxAssist {

    MANUAL(1, "Manual"),
    MANUAL_SUGGESTED_GEAR(2, "Manual + suggested gear"),
    AUTO(3, "Auto"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    GearboxAssist(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static GearboxAssist fromCode(int code) {
        for (GearboxAssist value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static GearboxAssist fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
