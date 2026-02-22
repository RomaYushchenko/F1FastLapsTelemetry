package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: m_recoveryMode (uint8). */
@Getter
public enum RecoveryMode {

    NONE(0, "None"),
    FLASHBACKS(1, "Flashbacks"),
    AUTO_RECOVERY(2, "Auto-recovery"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    RecoveryMode(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static RecoveryMode fromCode(int code) {
        for (RecoveryMode value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static RecoveryMode fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
