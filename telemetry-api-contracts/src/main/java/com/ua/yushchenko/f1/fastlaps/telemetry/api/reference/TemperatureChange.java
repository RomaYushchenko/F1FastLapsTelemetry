package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: track/air temperature change (m_trackTemperatureChange, m_airTemperatureChange, forecast).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum TemperatureChange {

    UP(0, "Up"),
    DOWN(1, "Down"),
    NO_CHANGE(2, "No change"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    TemperatureChange(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static TemperatureChange fromCode(int code) {
        for (TemperatureChange value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static TemperatureChange fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
