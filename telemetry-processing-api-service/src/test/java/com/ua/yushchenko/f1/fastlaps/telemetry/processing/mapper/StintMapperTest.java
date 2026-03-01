package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.StintDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StintMapper")
class StintMapperTest {

    private StintMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StintMapper();
    }

    @Test
    @DisplayName("toDto рахує avgLapTimeMs як середнє валідних кіл")
    void toDto_computesAvgLapTimeMs() {
        // Arrange
        List<Lap> laps = List.of(
                lapWithNumber(1, 87_100),
                lapWithNumber(2, 87_300),
                lapWithNumber(3, 0)
        );

        // Act
        StintDto dto = mapper.toDto(1, (int) COMPOUND_18, 1, 3, laps);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getStintIndex()).isEqualTo(1);
        assertThat(dto.getCompound()).isEqualTo(18);
        assertThat(dto.getStartLap()).isEqualTo(1);
        assertThat(dto.getLapCount()).isEqualTo(3);
        assertThat(dto.getAvgLapTimeMs()).isEqualTo(87_200); // (87100+87300)/2
        assertThat(dto.getDegradationIndicator()).isNull();
    }

    @Test
    @DisplayName("toDto повертає avgLapTimeMs null коли немає валідних кіл")
    void toDto_avgLapTimeMsNull_whenNoValidLaps() {
        // Arrange
        List<Lap> laps = List.of(lapWithNumber(1, 0), lapWithNumber(2, null));

        // Act
        StintDto dto = mapper.toDto(1, (int) COMPOUND_18, 1, 2, laps);

        // Assert
        assertThat(dto.getAvgLapTimeMs()).isNull();
    }

    @Test
    @DisplayName("toDto приймає порожній список laps")
    void toDto_acceptsEmptyLaps() {
        // Act
        StintDto dto = mapper.toDto(1, (int) COMPOUND_18, 1, 0, List.of());

        // Assert
        assertThat(dto.getAvgLapTimeMs()).isNull();
        assertThat(dto.getLapCount()).isEqualTo(0);
    }
}
