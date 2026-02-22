package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 track (m_trackId, int8). -1 = unknown.
 * Single source of truth for track id → display name. See: .github/draft/type-name-track-deep-research-report.md.
 */
@Getter
public enum F1Track {

    UNKNOWN(-1, "Unknown"),
    MELBOURNE(0, "Melbourne"),
    SHANGHAI(2, "Shanghai"),
    SAKHIR(3, "Sakhir (Bahrain)"),
    CATALUNYA(4, "Catalunya"),
    MONACO(5, "Monaco"),
    MONTREAL(6, "Montreal"),
    SILVERSTONE(7, "Silverstone"),
    HUNGARORING(9, "Hungaroring"),
    SPA(10, "Spa"),
    MONZA(11, "Monza"),
    SINGAPORE(12, "Singapore"),
    SUZUKA(13, "Suzuka"),
    ABU_DHABI(14, "Abu Dhabi"),
    TEXAS(15, "Texas"),
    BRAZIL(16, "Brazil"),
    AUSTRIA(17, "Austria"),
    MEXICO(19, "Mexico"),
    BAKU(20, "Baku (Azerbaijan)"),
    ZANDVOORT(26, "Zandvoort"),
    IMOLA(27, "Imola"),
    JEDDAH(29, "Jeddah"),
    MIAMI(30, "Miami"),
    LAS_VEGAS(31, "Las Vegas"),
    LOSAIL(32, "Losail"),
    SILVERSTONE_REVERSE(39, "Silverstone (Reverse)"),
    AUSTRIA_REVERSE(40, "Austria (Reverse)"),
    ZANDVOORT_REVERSE(41, "Zandvoort (Reverse)");

    private final int id;
    private final String displayName;

    F1Track(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Resolve track by F1 game id. Null, -1, or unknown id returns UNKNOWN.
     * Java byte -1 when read as unsigned becomes 255; both are treated as unknown.
     */
    public static F1Track fromId(Integer id) {
        if (id == null) {
            return UNKNOWN;
        }
        int v = id.intValue();
        if (v == -1 || v == 255) {
            return UNKNOWN;
        }
        for (F1Track value : values()) {
            if (value.id == v) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
