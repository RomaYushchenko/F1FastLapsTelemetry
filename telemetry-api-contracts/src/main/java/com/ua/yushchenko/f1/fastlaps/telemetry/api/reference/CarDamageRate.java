package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_carDamageRate (uint8). */
@Getter
public enum CarDamageRate {

    REDUCED(0, "Reduced"),
    STANDARD(1, "Standard"),
    SIMULATION(2, "Simulation"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    CarDamageRate(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static CarDamageRate fromCode(int code) {
        for (CarDamageRate value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static CarDamageRate fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
