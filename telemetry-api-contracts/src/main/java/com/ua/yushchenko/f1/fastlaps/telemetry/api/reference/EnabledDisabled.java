package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/** F1 25 PacketSessionData: 0=Disabled, 1=Enabled (e.g. m_collisionsOffForFirstLapOnly, m_mpOffForGriefing). */
@Getter
public enum EnabledDisabled {

    DISABLED(0, "Disabled"),
    ENABLED(1, "Enabled"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    EnabledDisabled(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static EnabledDisabled fromCode(int code) {
        for (EnabledDisabled value : values()) {
            if (value.code == code) return value;
        }
        return UNKNOWN;
    }

    public static EnabledDisabled fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
