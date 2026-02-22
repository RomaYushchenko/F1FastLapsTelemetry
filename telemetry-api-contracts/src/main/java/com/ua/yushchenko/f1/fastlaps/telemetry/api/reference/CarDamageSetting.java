package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_carDamage (uint8) – session setting. */
@Getter
public enum CarDamageSetting {

    OFF(0, "Off"),
    REDUCED(1, "Reduced"),
    STANDARD(2, "Standard"),
    SIMULATION(3, "Simulation"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    CarDamageSetting(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static CarDamageSetting fromCode(int code) {
        for (CarDamageSetting value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static CarDamageSetting fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
