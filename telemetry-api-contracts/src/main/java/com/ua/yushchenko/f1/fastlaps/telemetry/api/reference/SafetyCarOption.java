package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_safetyCar (uint8) – session setting (frequency). */
@Getter
public enum SafetyCarOption {

    OFF(0, "Off"),
    REDUCED(1, "Reduced"),
    STANDARD(2, "Standard"),
    INCREASED(3, "Increased"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SafetyCarOption(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SafetyCarOption fromCode(int code) {
        for (SafetyCarOption value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static SafetyCarOption fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
