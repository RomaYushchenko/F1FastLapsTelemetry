package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_dynamicRacingLine (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum DynamicRacingLine {

    OFF(0, "Off"),
    CORNERS_ONLY(1, "Corners only"),
    FULL(2, "Full"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    DynamicRacingLine(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static DynamicRacingLine fromCode(int code) {
        for (DynamicRacingLine value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DynamicRacingLine fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
