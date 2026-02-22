package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_networkGame (uint8).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum NetworkGame {

    OFFLINE(0, "Offline"),
    ONLINE(1, "Online"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    NetworkGame(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static NetworkGame fromCode(int code) {
        for (NetworkGame value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static NetworkGame fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
