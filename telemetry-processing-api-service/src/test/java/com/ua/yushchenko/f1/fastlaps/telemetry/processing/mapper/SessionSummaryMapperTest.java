package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionSummaryMapper")
class SessionSummaryMapperTest {

    private SessionSummaryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SessionSummaryMapper();
    }

    @Test
    @DisplayName("toDto повертає null коли summary null")
    void toDto_returnsNull_whenSummaryNull() {
        // Act
        SessionSummaryDto result = mapper.toDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDto мапить усі поля summary")
    void toDto_mapsAllFields() {
        // Arrange
        SessionSummary summary = sessionSummary();

        // Act
        SessionSummaryDto dto = mapper.toDto(summary);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getTotalLaps()).isEqualTo(10);
        assertThat(dto.getBestLapTimeMs()).isEqualTo(87_200);
        assertThat(dto.getBestLapNumber()).isEqualTo(3);
        assertThat(dto.getBestSector1Ms()).isEqualTo(27_900);
        assertThat(dto.getBestSector2Ms()).isEqualTo(30_400);
        assertThat(dto.getBestSector3Ms()).isEqualTo(28_900);
    }

    @Test
    @DisplayName("toDto коректно обробляє null числові поля")
    void toDto_handlesNullNumbers() {
        // Arrange
        SessionSummary summary = SessionSummary.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .totalLaps(null)
                .bestLapNumber(null)
                .lastUpdatedAt(ENDED_AT)
                .build();

        // Act
        SessionSummaryDto dto = mapper.toDto(summary);

        // Assert
        assertThat(dto.getTotalLaps()).isNull();
        assertThat(dto.getBestLapNumber()).isNull();
    }

    @Test
    @DisplayName("emptySummaryDto повертає DTO з нулем кола та null best-полями")
    void emptySummaryDto_returnsZeroTotalLapsAndNullBests() {
        // Act
        SessionSummaryDto dto = SessionSummaryMapper.emptySummaryDto();

        // Assert
        assertThat(dto.getTotalLaps()).isEqualTo(0);
        assertThat(dto.getBestLapTimeMs()).isNull();
        assertThat(dto.getBestLapNumber()).isNull();
        assertThat(dto.getBestSector1Ms()).isNull();
        assertThat(dto.getBestSector2Ms()).isNull();
        assertThat(dto.getBestSector3Ms()).isNull();
    }
}
