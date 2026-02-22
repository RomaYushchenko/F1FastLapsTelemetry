package com.ua.yushchenko.f1.fastlaps.telemetry.api.reference;

import lombok.Getter;

/**
 * F1 25 PacketLapData: m_resultStatus (invalid, inactive, active, finished, etc.).
 * See: .github/draft/other-enume-types-deep-research-report.md.
 */
@Getter
public enum ResultStatus {

    INVALID(0, "Invalid"),
    INACTIVE(1, "Inactive"),
    ACTIVE(2, "Active"),
    FINISHED(3, "Finished"),
    DID_NOT_FINISH(4, "Did not finish"),
    DISQUALIFIED(5, "Disqualified"),
    NOT_CLASSIFIED(6, "Not classified"),
    RETIRED(7, "Retired"),
    UNKNOWN(-1, "Unknown");

    private final int code;
    private final String displayName;

    ResultStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static ResultStatus fromCode(int code) {
        for (ResultStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static ResultStatus fromCode(Integer code) {
        return code == null ? UNKNOWN : fromCode(code.intValue());
    }
}
