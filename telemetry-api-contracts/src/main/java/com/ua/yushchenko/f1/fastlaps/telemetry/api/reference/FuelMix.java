package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 CarStatusData: m_fuelMix (0 = lean, 1 = standard, 2 = rich, 3 = max).
 * See: .github/draft/other-enume-types-deep-research-report.md, plan 11.
 */
@Getter
public enum FuelMix {

    LEAN(0, "Lean"),
    STANDARD(1, "Standard"),
    RICH(2, "Rich"),
    MAX(3, "Max"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    FuelMix(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static FuelMix fromCode(int code) {
        for (FuelMix value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static FuelMix fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
