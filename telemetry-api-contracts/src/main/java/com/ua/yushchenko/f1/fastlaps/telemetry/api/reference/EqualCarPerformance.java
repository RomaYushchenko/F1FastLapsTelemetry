package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_equalCarPerformance (uint8). */
@Getter
public enum EqualCarPerformance {

    OFF(0, "Off"),
    ON(1, "On"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    EqualCarPerformance(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static EqualCarPerformance fromCode(int code) {
        for (EqualCarPerformance value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static EqualCarPerformance fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
