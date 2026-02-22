package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_surfaceType (uint8). */
@Getter
public enum SurfaceType {

    SIMPLIFIED(0, "Simplified"),
    REALISTIC(1, "Realistic"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SurfaceType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SurfaceType fromCode(int code) {
        for (SurfaceType value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static SurfaceType fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
