package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;

/**
 * Maps F1 visual tyre compound codes to display (S/M/H or em dash) and persisted single-letter values.
 */
public final class TyreCompoundMapper {

    private static final String DISPLAY_UNKNOWN = "—";

    private TyreCompoundMapper() {
    }

    /**
     * Display string for API tables: "S", "M", "H", or em dash when unknown.
     */
    public static String toDisplayString(SessionRuntimeState.CarSnapshot snapshot) {
        if (snapshot == null) {
            return DISPLAY_UNKNOWN;
        }
        return toDisplayString(snapshot.getVisualTyreCompound());
    }

    /**
     * Display string from visual compound code, or em dash when null/unknown.
     */
    public static String toDisplayString(Integer visualTyreCompoundCode) {
        String persisted = toPersistedCompound(visualTyreCompoundCode);
        return persisted != null ? persisted : DISPLAY_UNKNOWN;
    }

    /**
     * Value for DB column {@code tyre_compound}: "S", "M", "H", or null when unknown.
     */
    public static String toPersistedCompound(SessionRuntimeState.CarSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return toPersistedCompound(snapshot.getVisualTyreCompound());
    }

    /**
     * Value for DB: "S", "M", "H", or null.
     */
    public static String toPersistedCompound(Integer visualTyreCompoundCode) {
        if (visualTyreCompoundCode == null) {
            return null;
        }
        int code = visualTyreCompoundCode;
        if (code <= 17) {
            return "S";
        }
        if (code <= 19) {
            return "M";
        }
        return "H";
    }

    /**
     * Map F1 actual tyre compound ({@code m_actualTyreCompound} / {@code tyre_wear_per_lap.compound}) to S/M/H.
     * C5–C4 (16–17) → S, C3–C2 (18–19) → M, C1 and above (20+) → H.
     * Inter (7) and Wet (8) return null so the UI shows an em dash, not a misclassified soft.
     */
    public static String toPersistedFromActualCompound(Short code) {
        if (code == null) {
            return null;
        }
        return toPersistedFromActualCompound(Short.toUnsignedInt(code));
    }

    /**
     * @see #toPersistedFromActualCompound(Short)
     */
    public static String toPersistedFromActualCompound(int unsignedActualCode) {
        if (unsignedActualCode == 7 || unsignedActualCode == 8) {
            return null;
        }
        if (unsignedActualCode >= 16 && unsignedActualCode <= 17) {
            return "S";
        }
        if (unsignedActualCode >= 18 && unsignedActualCode <= 19) {
            return "M";
        }
        if (unsignedActualCode >= 20) {
            return "H";
        }
        return null;
    }
}
