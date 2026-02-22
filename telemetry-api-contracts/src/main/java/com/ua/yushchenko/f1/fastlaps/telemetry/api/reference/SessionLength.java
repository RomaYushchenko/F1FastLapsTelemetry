package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_sessionLength (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SessionLength {

    NONE(0, "None"),
    VERY_SHORT(2, "Very Short"),
    SHORT(3, "Short"),
    MEDIUM(4, "Medium"),
    MEDIUM_LONG(5, "Medium Long"),
    LONG(6, "Long"),
    FULL(7, "Full"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SessionLength(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SessionLength fromCode(int code) {
        for (SessionLength value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SessionLength fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
