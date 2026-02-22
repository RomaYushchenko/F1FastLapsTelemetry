package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_speedUnitsLeadPlayer, m_speedUnitsSecondaryPlayer (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SpeedUnits {

    MPH(0, "MPH"),
    KPH(1, "KPH"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SpeedUnits(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SpeedUnits fromCode(int code) {
        for (SpeedUnits value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SpeedUnits fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
