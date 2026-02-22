package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DriverStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.PitLaneTimerActive;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.PitStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.ResultStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.Sector;

/**
 * Maps LapData (PacketLapData) raw codes to display names using reference enums (plan 10).
 * Single place for lap-data enum resolution when building REST DTOs, WebSocket messages, or logs.
 */
public final class LapDataMapper {

    private LapDataMapper() {
    }

    public static String pitStatusDisplayName(Integer code) {
        return PitStatus.fromCode(code).getDisplayName();
    }

    public static String sectorDisplayName(Integer code) {
        return Sector.fromCode(code).getDisplayName();
    }

    public static String driverStatusDisplayName(Integer code) {
        return DriverStatus.fromCode(code).getDisplayName();
    }

    public static String resultStatusDisplayName(Integer code) {
        return ResultStatus.fromCode(code).getDisplayName();
    }

    public static String pitLaneTimerActiveDisplayName(Integer code) {
        return PitLaneTimerActive.fromCode(code).getDisplayName();
    }
}
