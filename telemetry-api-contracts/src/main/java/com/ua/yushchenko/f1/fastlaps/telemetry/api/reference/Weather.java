package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: weather (m_weather, weather forecast samples).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum Weather {

    CLEAR(0, "Clear"),
    LIGHT_CLOUD(1, "Light cloud"),
    OVERCAST(2, "Overcast"),
    LIGHT_RAIN(3, "Light rain"),
    HEAVY_RAIN(4, "Heavy rain"),
    STORM(5, "Storm"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    Weather(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Weather fromCode(int code) {
        for (Weather value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static Weather fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
