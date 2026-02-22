package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_dynamicRacingLineType (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum DynamicRacingLineType {

    TWO_D(0, "2D"),
    THREE_D(1, "3D"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    DynamicRacingLineType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static DynamicRacingLineType fromCode(int code) {
        for (DynamicRacingLineType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DynamicRacingLineType fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
