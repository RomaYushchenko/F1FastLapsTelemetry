package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_pitLaneTyreSim (uint8). Report: 0=On, 1=Off. */
@Getter
public enum PitLaneTyreSim {

    ON(0, "On"),
    OFF(1, "Off"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    PitLaneTyreSim(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PitLaneTyreSim fromCode(int code) {
        for (PitLaneTyreSim value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static PitLaneTyreSim fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
