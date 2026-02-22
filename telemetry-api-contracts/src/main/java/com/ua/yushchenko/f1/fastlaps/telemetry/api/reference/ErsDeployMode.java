package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 CarStatusData: m_ersDeployMode (0 = none, 1 = medium, 2 = hotlap, 3 = overtake).
 * See: .github/draft/other-enume-types-deep-research-report.md, plan 11.
 */
@Getter
public enum ErsDeployMode {

    NONE(0, "None"),
    MEDIUM(1, "Medium"),
    HOTLAP(2, "Hotlap"),
    OVERTAKE(3, "Overtake"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    ErsDeployMode(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static ErsDeployMode fromCode(int code) {
        for (ErsDeployMode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static ErsDeployMode fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }

    /** True if driver is deploying ERS (any mode other than NONE). */
    public boolean isDeployActive() {
        return this != NONE && this != UNKNOWN;
    }
}
