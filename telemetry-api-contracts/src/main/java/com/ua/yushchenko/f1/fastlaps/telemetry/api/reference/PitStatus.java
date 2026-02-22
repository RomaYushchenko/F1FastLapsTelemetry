package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketLapData: m_pitStatus (0 = none, 1 = pitting, 2 = in pit area).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum PitStatus {

    NONE(0, "None"),
    PITTING(1, "Pitting"),
    IN_PIT_AREA(2, "In pit area"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    PitStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PitStatus fromCode(int code) {
        for (PitStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static PitStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
