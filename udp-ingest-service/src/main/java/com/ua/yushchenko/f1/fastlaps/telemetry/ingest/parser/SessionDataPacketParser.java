package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataDto;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parses F1 25 PacketSessionData (packet-specific payload, 724 bytes) into {@link SessionDataDto}.
 * Layout per .github/docs/F1 25 Telemetry Output Structures.txt (Session - 753 bytes total, header 29).
 */
@Component
public class SessionDataPacketParser {

    /** PacketSessionData payload size (753 - 29 header). */
    public static final int SESSION_DATA_PAYLOAD_SIZE = 724;
    private static final int MARSHAL_ZONES = 21;
    private static final int WEATHER_FORECAST_SAMPLES = 64;
    private static final int WEEKEND_STRUCTURE_LEN = 12;

    /**
     * Parse full session data from current buffer position. Buffer must have at least 724 bytes remaining.
     * Advances buffer position by 724 bytes.
     */
    public SessionDataDto parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int weather = buffer.get() & 0xFF;
        int trackTemperature = buffer.get();
        int airTemperature = buffer.get();
        int totalLaps = buffer.get() & 0xFF;
        int trackLength = buffer.getShort() & 0xFFFF;
        int sessionType = buffer.get() & 0xFF;
        int trackId = buffer.get();
        int formula = buffer.get() & 0xFF;
        int sessionTimeLeft = buffer.getShort() & 0xFFFF;
        int sessionDuration = buffer.getShort() & 0xFFFF;
        int pitSpeedLimit = buffer.get() & 0xFF;
        int gamePaused = buffer.get() & 0xFF;
        int isSpectating = buffer.get() & 0xFF;
        int spectatorCarIndex = buffer.get() & 0xFF;
        int sliProNativeSupport = buffer.get() & 0xFF;
        int numMarshalZones = buffer.get() & 0xFF;

        float[] marshalZoneStarts = new float[MARSHAL_ZONES];
        int[] marshalZoneFlags = new int[MARSHAL_ZONES];
        for (int i = 0; i < MARSHAL_ZONES; i++) {
            marshalZoneStarts[i] = buffer.getFloat();
            marshalZoneFlags[i] = buffer.get();
        }

        int safetyCarStatus = buffer.get() & 0xFF;
        int networkGame = buffer.get() & 0xFF;
        int numWeatherForecastSamples = buffer.get() & 0xFF;

        int[] wfSessionType = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfTimeOffset = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfWeather = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfTrackTemp = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfTrackTempChange = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfAirTemp = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfAirTempChange = new int[WEATHER_FORECAST_SAMPLES];
        int[] wfRainPct = new int[WEATHER_FORECAST_SAMPLES];
        for (int i = 0; i < WEATHER_FORECAST_SAMPLES; i++) {
            wfSessionType[i] = buffer.get() & 0xFF;
            wfTimeOffset[i] = buffer.get() & 0xFF;
            wfWeather[i] = buffer.get() & 0xFF;
            wfTrackTemp[i] = buffer.get();
            wfTrackTempChange[i] = buffer.get();
            wfAirTemp[i] = buffer.get();
            wfAirTempChange[i] = buffer.get();
            wfRainPct[i] = buffer.get() & 0xFF;
        }

        int forecastAccuracy = buffer.get() & 0xFF;
        int aiDifficulty = buffer.get() & 0xFF;
        long seasonLinkIdentifier = buffer.getInt() & 0xFFFFFFFFL;
        long weekendLinkIdentifier = buffer.getInt() & 0xFFFFFFFFL;
        long sessionLinkIdentifier = buffer.getInt() & 0xFFFFFFFFL;
        int pitStopWindowIdealLap = buffer.get() & 0xFF;
        int pitStopWindowLatestLap = buffer.get() & 0xFF;
        int pitStopRejoinPosition = buffer.get() & 0xFF;

        int steeringAssist = buffer.get() & 0xFF;
        int brakingAssist = buffer.get() & 0xFF;
        int gearboxAssist = buffer.get() & 0xFF;
        int pitAssist = buffer.get() & 0xFF;
        int pitReleaseAssist = buffer.get() & 0xFF;
        int ersAssist = buffer.get() & 0xFF;
        int drsAssist = buffer.get() & 0xFF;
        int dynamicRacingLine = buffer.get() & 0xFF;
        int dynamicRacingLineType = buffer.get() & 0xFF;
        int gameMode = buffer.get() & 0xFF;
        int ruleSet = buffer.get() & 0xFF;
        long timeOfDay = buffer.getInt() & 0xFFFFFFFFL;
        int sessionLength = buffer.get() & 0xFF;
        int speedUnitsLeadPlayer = buffer.get() & 0xFF;
        int temperatureUnitsLeadPlayer = buffer.get() & 0xFF;
        int speedUnitsSecondaryPlayer = buffer.get() & 0xFF;
        int temperatureUnitsSecondaryPlayer = buffer.get() & 0xFF;
        int numSafetyCarPeriods = buffer.get() & 0xFF;
        int numVirtualSafetyCarPeriods = buffer.get() & 0xFF;
        int numRedFlagPeriods = buffer.get() & 0xFF;
        int equalCarPerformance = buffer.get() & 0xFF;
        int recoveryMode = buffer.get() & 0xFF;
        int flashbackLimit = buffer.get() & 0xFF;
        int surfaceType = buffer.get() & 0xFF;
        int lowFuelMode = buffer.get() & 0xFF;
        int raceStarts = buffer.get() & 0xFF;
        int tyreTemperature = buffer.get() & 0xFF;
        int pitLaneTyreSim = buffer.get() & 0xFF;
        int carDamage = buffer.get() & 0xFF;
        int carDamageRate = buffer.get() & 0xFF;
        int collisions = buffer.get() & 0xFF;
        int collisionsOffForFirstLapOnly = buffer.get() & 0xFF;
        int mpUnsafePitRelease = buffer.get() & 0xFF;
        int mpOffForGriefing = buffer.get() & 0xFF;
        int cornerCuttingStringency = buffer.get() & 0xFF;
        int parcFermeRules = buffer.get() & 0xFF;
        int pitStopExperience = buffer.get() & 0xFF;
        int safetyCar = buffer.get() & 0xFF;
        int safetyCarExperience = buffer.get() & 0xFF;
        int formationLap = buffer.get() & 0xFF;
        int formationLapExperience = buffer.get() & 0xFF;
        int redFlags = buffer.get() & 0xFF;
        int affectsLicenceLevelSolo = buffer.get() & 0xFF;
        int affectsLicenceLevelMP = buffer.get() & 0xFF;
        int numSessionsInWeekend = buffer.get() & 0xFF;

        int[] weekendStructure = new int[WEEKEND_STRUCTURE_LEN];
        for (int i = 0; i < WEEKEND_STRUCTURE_LEN; i++) {
            weekendStructure[i] = buffer.get() & 0xFF;
        }
        float sector2LapDistanceStart = buffer.getFloat();
        float sector3LapDistanceStart = buffer.getFloat();

        return SessionDataDto.builder()
                .weather(weather)
                .trackTemperature(trackTemperature)
                .airTemperature(airTemperature)
                .totalLaps(totalLaps)
                .trackLength(trackLength)
                .sessionType(sessionType)
                .trackId(trackId)
                .formula(formula)
                .sessionTimeLeft(sessionTimeLeft)
                .sessionDuration(sessionDuration)
                .pitSpeedLimit(pitSpeedLimit)
                .gamePaused(gamePaused)
                .isSpectating(isSpectating)
                .spectatorCarIndex(spectatorCarIndex)
                .sliProNativeSupport(sliProNativeSupport)
                .numMarshalZones(numMarshalZones)
                .marshalZoneStarts(marshalZoneStarts)
                .marshalZoneFlags(marshalZoneFlags)
                .safetyCarStatus(safetyCarStatus)
                .networkGame(networkGame)
                .numWeatherForecastSamples(numWeatherForecastSamples)
                .weatherForecastSessionType(wfSessionType)
                .weatherForecastTimeOffset(wfTimeOffset)
                .weatherForecastWeather(wfWeather)
                .weatherForecastTrackTemperature(wfTrackTemp)
                .weatherForecastTrackTemperatureChange(wfTrackTempChange)
                .weatherForecastAirTemperature(wfAirTemp)
                .weatherForecastAirTemperatureChange(wfAirTempChange)
                .weatherForecastRainPercentage(wfRainPct)
                .forecastAccuracy(forecastAccuracy)
                .aiDifficulty(aiDifficulty)
                .seasonLinkIdentifier(seasonLinkIdentifier)
                .weekendLinkIdentifier(weekendLinkIdentifier)
                .sessionLinkIdentifier(sessionLinkIdentifier)
                .pitStopWindowIdealLap(pitStopWindowIdealLap)
                .pitStopWindowLatestLap(pitStopWindowLatestLap)
                .pitStopRejoinPosition(pitStopRejoinPosition)
                .steeringAssist(steeringAssist)
                .brakingAssist(brakingAssist)
                .gearboxAssist(gearboxAssist)
                .pitAssist(pitAssist)
                .pitReleaseAssist(pitReleaseAssist)
                .ersAssist(ersAssist)
                .drsAssist(drsAssist)
                .dynamicRacingLine(dynamicRacingLine)
                .dynamicRacingLineType(dynamicRacingLineType)
                .gameMode(gameMode)
                .ruleSet(ruleSet)
                .timeOfDay(timeOfDay)
                .sessionLength(sessionLength)
                .speedUnitsLeadPlayer(speedUnitsLeadPlayer)
                .temperatureUnitsLeadPlayer(temperatureUnitsLeadPlayer)
                .speedUnitsSecondaryPlayer(speedUnitsSecondaryPlayer)
                .temperatureUnitsSecondaryPlayer(temperatureUnitsSecondaryPlayer)
                .numSafetyCarPeriods(numSafetyCarPeriods)
                .numVirtualSafetyCarPeriods(numVirtualSafetyCarPeriods)
                .numRedFlagPeriods(numRedFlagPeriods)
                .equalCarPerformance(equalCarPerformance)
                .recoveryMode(recoveryMode)
                .flashbackLimit(flashbackLimit)
                .surfaceType(surfaceType)
                .lowFuelMode(lowFuelMode)
                .raceStarts(raceStarts)
                .tyreTemperature(tyreTemperature)
                .pitLaneTyreSim(pitLaneTyreSim)
                .carDamage(carDamage)
                .carDamageRate(carDamageRate)
                .collisions(collisions)
                .collisionsOffForFirstLapOnly(collisionsOffForFirstLapOnly)
                .mpUnsafePitRelease(mpUnsafePitRelease)
                .mpOffForGriefing(mpOffForGriefing)
                .cornerCuttingStringency(cornerCuttingStringency)
                .parcFermeRules(parcFermeRules)
                .pitStopExperience(pitStopExperience)
                .safetyCar(safetyCar)
                .safetyCarExperience(safetyCarExperience)
                .formationLap(formationLap)
                .formationLapExperience(formationLapExperience)
                .redFlags(redFlags)
                .affectsLicenceLevelSolo(affectsLicenceLevelSolo)
                .affectsLicenceLevelMP(affectsLicenceLevelMP)
                .numSessionsInWeekend(numSessionsInWeekend)
                .weekendStructure(weekendStructure)
                .sector2LapDistanceStart(sector2LapDistanceStart)
                .sector3LapDistanceStart(sector3LapDistanceStart)
                .build();
    }
}
