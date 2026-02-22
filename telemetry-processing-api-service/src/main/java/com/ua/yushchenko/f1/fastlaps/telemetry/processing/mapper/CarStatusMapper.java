package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DrsState;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.ErsDeployMode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.FaultStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.FuelMix;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.PitLimiterStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.TractionControl;

/**
 * Maps Car Telemetry/Status/Damage raw codes to display names using reference enums (plan 11).
 * Single place for car-status enum resolution when building REST DTOs, WebSocket messages, or logs.
 */
public final class CarStatusMapper {

    private CarStatusMapper() {
    }

    public static String drsStateDisplayName(Integer code) {
        return DrsState.fromCode(code).getDisplayName();
    }

    public static String ersDeployModeDisplayName(Integer code) {
        return ErsDeployMode.fromCode(code).getDisplayName();
    }

    public static String faultStatusDisplayName(Integer code) {
        return FaultStatus.fromCode(code).getDisplayName();
    }

    public static String tractionControlDisplayName(Integer code) {
        return TractionControl.fromCode(code).getDisplayName();
    }

    public static String fuelMixDisplayName(Integer code) {
        return FuelMix.fromCode(code).getDisplayName();
    }

    public static String pitLimiterStatusDisplayName(Integer code) {
        return PitLimiterStatus.fromCode(code).getDisplayName();
    }
}
