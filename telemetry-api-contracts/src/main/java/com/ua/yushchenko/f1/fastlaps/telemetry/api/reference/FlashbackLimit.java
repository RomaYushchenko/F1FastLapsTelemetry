package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_flashbackLimit (uint8). */
@Getter
public enum FlashbackLimit {

    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    UNLIMITED(3, "Unlimited"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    FlashbackLimit(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static FlashbackLimit fromCode(int code) {
        for (FlashbackLimit value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static FlashbackLimit fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
