package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketLapData: m_sector (0 = sector1, 1 = sector2, 2 = sector3).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum Sector {

    SECTOR_1(0, "Sector 1"),
    SECTOR_2(1, "Sector 2"),
    SECTOR_3(2, "Sector 3"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    Sector(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Sector fromCode(int code) {
        for (Sector value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static Sector fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
