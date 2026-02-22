package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_cornerCuttingStringency (uint8). */
@Getter
public enum CornerCuttingStringency {

    REGULAR(0, "Regular"),
    STRICT(1, "Strict"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    CornerCuttingStringency(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static CornerCuttingStringency fromCode(int code) {
        for (CornerCuttingStringency value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static CornerCuttingStringency fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
