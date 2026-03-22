package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LapMapper")
class LapMapperTest {

    private LapMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LapMapper();
    }

    @Test
    @DisplayName("toLapResponseDto повертає null коли lap null")
    void toLapResponseDto_returnsNull_whenLapNull() {
        // Act
        LapResponseDto result = mapper.toLapResponseDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toLapResponseDto мапить усі поля коректно")
    void toLapResponseDto_mapsAllFields() {
        // Arrange
        Lap lap = lap();

        // Act
        LapResponseDto dto = mapper.toLapResponseDto(lap);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getLapNumber()).isEqualTo(LAP_NUMBER);
        assertThat(dto.getLapTimeMs()).isEqualTo(LAP_TIME_MS);
        assertThat(dto.getSector1Ms()).isEqualTo(SECTOR1_MS);
        assertThat(dto.getSector2Ms()).isEqualTo(SECTOR2_MS);
        assertThat(dto.getSector3Ms()).isEqualTo(SECTOR3_MS);
        assertThat(dto.isInvalid()).isFalse();
        assertThat(dto.getPositionAtLapStart()).isEqualTo(3);
    }

    @Test
    @DisplayName("toLapResponseDto мапить invalid lap")
    void toLapResponseDto_mapsInvalidLap() {
        // Act
        LapResponseDto dto = mapper.toLapResponseDto(lapInvalid());

        // Assert
        assertThat(dto.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("toPacePointDto повертає null коли lap null")
    void toPacePointDto_returnsNull_whenLapNull() {
        // Act
        PacePointDto result = mapper.toPacePointDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toPacePointDto повертає null коли lapTimeMs нуль")
    void toPacePointDto_returnsNull_whenLapTimeMsZero() {
        // Act
        PacePointDto result = mapper.toPacePointDto(lapZeroTime());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toPacePointDto повертає null коли lapTimeMs null")
    void toPacePointDto_returnsNull_whenLapTimeMsNull() {
        // Arrange
        Lap lap = lap();
        lap.setLapTimeMs(null);

        // Act
        PacePointDto result = mapper.toPacePointDto(lap);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toPacePointDto мапить валідний lap")
    void toPacePointDto_mapsValidLap() {
        // Act
        PacePointDto dto = mapper.toPacePointDto(lap());

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getLapNumber()).isEqualTo(LAP_NUMBER);
        assertThat(dto.getLapTimeMs()).isEqualTo(LAP_TIME_MS);
    }

    @Test
    @DisplayName("toTracePointDto повертає null коли row null")
    void toTracePointDto_returnsNull_whenRowNull() {
        // Act
        TracePointDto result = mapper.toTracePointDto((CarTelemetryRaw) null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toTracePointDto мапить усі поля")
    void toTracePointDto_mapsAllFields() {
        // Arrange
        CarTelemetryRaw raw = carTelemetryRaw();

        // Act
        TracePointDto dto = mapper.toTracePointDto(raw);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getDistance()).isEqualTo(LAP_DISTANCE_M);
        assertThat(dto.getThrottle()).isEqualTo(THROTTLE);
        assertThat(dto.getBrake()).isEqualTo(BRAKE);
    }

    @Test
    @DisplayName("toSpeedTracePointDto повертає null коли row null")
    void toSpeedTracePointDto_returnsNull_whenRowNull() {
        // Act
        SpeedTracePointDto result = mapper.toSpeedTracePointDto((CarTelemetryRaw) null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toSpeedTracePointDto повертає null коли speed null")
    void toSpeedTracePointDto_returnsNull_whenSpeedNull() {
        CarTelemetryRaw noSpeed = CarTelemetryRaw.builder()
                .sessionUid(SESSION_UID).frameIdentifier(FRAME_ID).carIndex(CAR_INDEX).ts(RAW_TS)
                .lapDistanceM(LAP_DISTANCE_M).speedKph(null).build();

        assertThat(mapper.toSpeedTracePointDto(noSpeed)).isNull();
    }

    @Test
    @DisplayName("toSpeedTracePointDto підставляє синтетичну відстань коли lapDistanceM null")
    void toSpeedTracePointDto_usesSyntheticDistance_whenLapDistanceNull() {
        CarTelemetryRaw noDistance = CarTelemetryRaw.builder()
                .sessionUid(SESSION_UID).frameIdentifier(FRAME_ID).carIndex(CAR_INDEX).ts(RAW_TS)
                .lapDistanceM(null).speedKph(SPEED_KPH).build();

        SpeedTracePointDto at0 = mapper.toSpeedTracePointDto(noDistance, 0);
        SpeedTracePointDto at4 = mapper.toSpeedTracePointDto(noDistance, 4);

        assertThat(at0).isNotNull();
        assertThat(at0.getDistanceM()).isEqualTo(0f);
        assertThat(at0.getSpeedKph()).isEqualTo(SPEED_KPH);
        assertThat(at4.getDistanceM()).isEqualTo(20f);
    }

    @Test
    @DisplayName("toSpeedTracePointDto мапить distance та speed")
    void toSpeedTracePointDto_mapsDistanceAndSpeed() {
        // Arrange
        CarTelemetryRaw raw = carTelemetryRaw();

        // Act
        SpeedTracePointDto dto = mapper.toSpeedTracePointDto(raw);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getDistanceM()).isEqualTo(LAP_DISTANCE_M);
        assertThat(dto.getSpeedKph()).isEqualTo(SPEED_KPH);
    }

    @Test
    @DisplayName("toTracePointDto обробляє null поля як нулі")
    void toTracePointDto_handlesNullFields() {
        // Arrange
        CarTelemetryRaw raw = CarTelemetryRaw.builder()
                .sessionUid(SESSION_UID)
                .frameIdentifier(FRAME_ID)
                .carIndex(CAR_INDEX)
                .ts(RAW_TS)
                .build();

        // Act
        TracePointDto dto = mapper.toTracePointDto(raw);

        // Assert
        assertThat(dto.getDistance()).isEqualTo(0.0);
        assertThat(dto.getThrottle()).isEqualTo(0.0);
        assertThat(dto.getBrake()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("toTyreWearPointDto повертає null коли row null")
    void toTyreWearPointDto_returnsNull_whenRowNull() {
        // Act
        TyreWearPointDto result = mapper.toTyreWearPointDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toTyreWearPointDto мапить усі поля зносу шин")
    void toTyreWearPointDto_mapsAllFields() {
        // Act
        TyreWearPointDto dto = mapper.toTyreWearPointDto(tyreWearPerLap());

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getLapNumber()).isEqualTo(LAP_NUMBER);
        assertThat(dto.getWearFL()).isEqualTo(WEAR_FL);
        assertThat(dto.getWearFR()).isEqualTo(WEAR_FR);
        assertThat(dto.getWearRL()).isEqualTo(WEAR_RL);
        assertThat(dto.getWearRR()).isEqualTo(WEAR_RR);
        assertThat(dto.getCompound()).isEqualTo(18);
    }
}
