package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_mpUnsafePitRelease (uint8). Report: 0=On, 1=Off. */
@Getter
public enum MpUnsafePitRelease {

    ON(0, "On"),
    OFF(1, "Off"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    MpUnsafePitRelease(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static MpUnsafePitRelease fromCode(int code) {
        for (MpUnsafePitRelease value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static MpUnsafePitRelease fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
