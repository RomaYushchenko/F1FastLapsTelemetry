package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: m_formula (uint8).
 * See: .github/docs/F1 25 Telemetry Output Structures.txt, .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum Formula {

    F1_MODERN(0, "F1 Modern"),
    F1_CLASSIC(1, "F1 Classic"),
    F2(2, "F2"),
    F1_GENERIC(3, "F1 Generic"),
    BETA(4, "Beta"),
    ESPORTS(6, "Esports"),
    F1_WORLD(8, "F1 World"),
    F1_ELIMINATION(9, "F1 Elimination"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    Formula(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Formula fromCode(int code) {
        for (Formula value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static Formula fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
