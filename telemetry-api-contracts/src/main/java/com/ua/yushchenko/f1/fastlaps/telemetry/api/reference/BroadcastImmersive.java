package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: 0=Broadcast, 1=Immersive (m_safetyCarExperience, m_formationLapExperience). */
@Getter
public enum BroadcastImmersive {

    BROADCAST(0, "Broadcast"),
    IMMERSIVE(1, "Immersive"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    BroadcastImmersive(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static BroadcastImmersive fromCode(int code) {
        for (BroadcastImmersive value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static BroadcastImmersive fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
