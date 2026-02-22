package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 CarDamageData: m_drsFault, m_ersFault (0 = OK, 1 = fault).
 * See: .github/draft/other-enume-types-deep-research-report.md, plan 11.
 */
@Getter
public enum FaultStatus {

    OK(0, "OK"),
    FAULT(1, "Fault"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    FaultStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static FaultStatus fromCode(int code) {
        for (FaultStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static FaultStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
