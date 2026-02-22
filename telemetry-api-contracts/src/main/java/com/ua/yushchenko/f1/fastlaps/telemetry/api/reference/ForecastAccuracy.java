package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_forecastAccuracy (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum ForecastAccuracy {

    PERFECT(0, "Perfect"),
    APPROXIMATE(1, "Approximate"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    ForecastAccuracy(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static ForecastAccuracy fromCode(int code) {
        for (ForecastAccuracy value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static ForecastAccuracy fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
