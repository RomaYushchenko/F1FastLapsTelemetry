package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_temperatureUnitsLeadPlayer, m_temperatureUnitsSecondaryPlayer (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum TemperatureUnits {

    CELSIUS(0, "Celsius"),
    FAHRENHEIT(1, "Fahrenheit"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    TemperatureUnits(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static TemperatureUnits fromCode(int code) {
        for (TemperatureUnits value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static TemperatureUnits fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
