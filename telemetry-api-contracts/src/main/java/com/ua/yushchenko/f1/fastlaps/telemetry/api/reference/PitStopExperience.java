package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_pitStopExperience (uint8). */
@Getter
public enum PitStopExperience {

    AUTOMATIC(0, "Automatic"),
    BROADCAST(1, "Broadcast"),
    IMMERSIVE(2, "Immersive"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    PitStopExperience(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PitStopExperience fromCode(int code) {
        for (PitStopExperience value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static PitStopExperience fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
