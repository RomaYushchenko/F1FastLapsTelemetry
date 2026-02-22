package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.AssistOnOff;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.BrakingAssist;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DynamicRacingLine;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DynamicRacingLineType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.ForecastAccuracy;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.Formula;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.GearboxAssist;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.NetworkGame;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SafetyCarStatus;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SessionLength;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SliProSupport;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SpeedUnits;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.SteeringAssist;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.TemperatureUnits;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.Weather;
/**
 * Maps SessionDataDto raw codes to display names using reference enums (plan 09).
 * Single place for session packet enum resolution when building REST DTOs or logs.
 * Session type and track remain in {@link SessionMapper} via F1SessionType and F1Track.
 */
public final class SessionDataMapper {

    private SessionDataMapper() {
    }

    public static String weatherDisplayName(Integer code) {
        return Weather.fromCode(code).getDisplayName();
    }

    public static String formulaDisplayName(Integer code) {
        return Formula.fromCode(code).getDisplayName();
    }

    public static String safetyCarStatusDisplayName(Integer code) {
        return SafetyCarStatus.fromCode(code).getDisplayName();
    }

    public static String sessionLengthDisplayName(Integer code) {
        return SessionLength.fromCode(code).getDisplayName();
    }

    public static String sliProSupportDisplayName(Integer code) {
        return SliProSupport.fromCode(code).getDisplayName();
    }

    public static String networkGameDisplayName(Integer code) {
        return NetworkGame.fromCode(code).getDisplayName();
    }

    public static String forecastAccuracyDisplayName(Integer code) {
        return ForecastAccuracy.fromCode(code).getDisplayName();
    }

    public static String steeringAssistDisplayName(Integer code) {
        return SteeringAssist.fromCode(code).getDisplayName();
    }

    public static String brakingAssistDisplayName(Integer code) {
        return BrakingAssist.fromCode(code).getDisplayName();
    }

    public static String gearboxAssistDisplayName(Integer code) {
        return GearboxAssist.fromCode(code).getDisplayName();
    }

    public static String assistOnOffDisplayName(Integer code) {
        return AssistOnOff.fromCode(code).getDisplayName();
    }

    public static String dynamicRacingLineDisplayName(Integer code) {
        return DynamicRacingLine.fromCode(code).getDisplayName();
    }

    public static String dynamicRacingLineTypeDisplayName(Integer code) {
        return DynamicRacingLineType.fromCode(code).getDisplayName();
    }

    public static String speedUnitsDisplayName(Integer code) {
        return SpeedUnits.fromCode(code).getDisplayName();
    }

    public static String temperatureUnitsDisplayName(Integer code) {
        return TemperatureUnits.fromCode(code).getDisplayName();
    }
}
