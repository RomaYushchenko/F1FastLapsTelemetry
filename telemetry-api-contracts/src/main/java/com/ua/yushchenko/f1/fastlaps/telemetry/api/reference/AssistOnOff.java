package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketSessionData: on/off assist fields (m_pitAssist, m_pitReleaseAssist, m_ERSAssist, m_DRSAssist, etc.).
 * Use for pitAssist, pitReleaseAssist, ersAssist, drsAssist and other 0=off/1=on session options.
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum AssistOnOff {

    OFF(0, "Off"),
    ON(1, "On"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    AssistOnOff(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static AssistOnOff fromCode(int code) {
        for (AssistOnOff value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static AssistOnOff fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
