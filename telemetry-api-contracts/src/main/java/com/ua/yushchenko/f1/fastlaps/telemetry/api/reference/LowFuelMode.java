package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_lowFuelMode (uint8). */
@Getter
public enum LowFuelMode {

    EASY(0, "Easy"),
    HARD(1, "Hard"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    LowFuelMode(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static LowFuelMode fromCode(int code) {
        for (LowFuelMode value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static LowFuelMode fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
