package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: MarshalZone.m_zoneFlag (int8).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum MarshalZoneFlag {

    INVALID(-1, "Invalid"),
    NONE(0, "None"),
    GREEN(1, "Green"),
    BLUE(2, "Blue"),
    YELLOW(3, "Yellow"),
    UNKNOWN(127, "Unknown");

    private final int code;
    private final String displayName;

    MarshalZoneFlag(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static MarshalZoneFlag fromCode(int code) {
        for (MarshalZoneFlag value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static MarshalZoneFlag fromCode(Integer code) {
        return code == null ? INVALID : fromCode(code.intValue());
    }
}
