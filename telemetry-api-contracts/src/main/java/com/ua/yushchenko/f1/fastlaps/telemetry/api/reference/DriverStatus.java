package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketLapData: m_driverStatus (0 = in garage, 1 = flying lap, 2 = in lap, 3 = out lap, 4 = on track).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum DriverStatus {

    IN_GARAGE(0, "In garage"),
    FLYING_LAP(1, "Flying lap"),
    IN_LAP(2, "In lap"),
    OUT_LAP(3, "Out lap"),
    ON_TRACK(4, "On track"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    DriverStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static DriverStatus fromCode(int code) {
        for (DriverStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DriverStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
