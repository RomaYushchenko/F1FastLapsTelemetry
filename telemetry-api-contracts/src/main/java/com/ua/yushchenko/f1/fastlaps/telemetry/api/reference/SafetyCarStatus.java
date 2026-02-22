package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_safetyCarStatus (uint8).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum SafetyCarStatus {

    NONE(0, "No safety car"),
    FULL(1, "Full safety car"),
    VIRTUAL(2, "Virtual safety car"),
    FORMATION_LAP(3, "Formation lap"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    SafetyCarStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SafetyCarStatus fromCode(int code) {
        for (SafetyCarStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static SafetyCarStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
