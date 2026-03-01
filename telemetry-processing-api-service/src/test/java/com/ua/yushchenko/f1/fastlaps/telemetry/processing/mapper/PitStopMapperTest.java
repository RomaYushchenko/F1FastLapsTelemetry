package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PitStopDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PitStopMapper")
class PitStopMapperTest {

    private PitStopMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PitStopMapper();
    }

    @Test
    @DisplayName("toDto повертає null коли outLap null")
    void toDto_returnsNull_whenOutLapNull() {
        // Arrange
        Lap inLap = lapWithNumber(1, 92_500);

        // Act
        PitStopDto result = mapper.toDto(inLap, null, (int) COMPOUND_18, (int) COMPOUND_16);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDto мапить усі поля з inLap та outLap")
    void toDto_mapsAllFields() {
        // Arrange
        Lap inLap = lapWithNumber(1, 92_500);
        Lap outLap = lapWithNumber(2, 95_200);

        // Act
        PitStopDto dto = mapper.toDto(inLap, outLap, (int) COMPOUND_18, (int) COMPOUND_16);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getLapNumber()).isEqualTo(2);
        assertThat(dto.getInLapTimeMs()).isEqualTo(92_500);
        assertThat(dto.getPitDurationMs()).isNull();
        assertThat(dto.getOutLapTimeMs()).isEqualTo(95_200);
        assertThat(dto.getCompoundIn()).isEqualTo(18);
        assertThat(dto.getCompoundOut()).isEqualTo(16);
    }

    @Test
    @DisplayName("toDto приймає null inLap (перший коло піт)")
    void toDto_acceptsNullInLap() {
        // Arrange
        Lap outLap = lapWithNumber(1, 95_200);

        // Act
        PitStopDto dto = mapper.toDto(null, outLap, null, (int) COMPOUND_16);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getLapNumber()).isEqualTo(1);
        assertThat(dto.getInLapTimeMs()).isNull();
        assertThat(dto.getOutLapTimeMs()).isEqualTo(95_200);
        assertThat(dto.getCompoundIn()).isNull();
        assertThat(dto.getCompoundOut()).isEqualTo(16);
    }
}
