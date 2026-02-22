package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LapDataMapper")
class LapDataMapperTest {

    @Test
    @DisplayName("pitStatusDisplayName повертає display name для коду 1")
    void pitStatusDisplayName_returnsDisplayName_forCode1() {
        // Act
        String result = LapDataMapper.pitStatusDisplayName(1);

        // Assert
        assertThat(result).isEqualTo("Pitting");
    }

    @Test
    @DisplayName("pitStatusDisplayName повертає Unknown для null")
    void pitStatusDisplayName_returnsUnknown_forNull() {
        // Act
        String result = LapDataMapper.pitStatusDisplayName(null);

        // Assert
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("sectorDisplayName повертає display name для коду 0")
    void sectorDisplayName_returnsDisplayName_forCode0() {
        // Act
        String result = LapDataMapper.sectorDisplayName(0);

        // Assert
        assertThat(result).isEqualTo("Sector 1");
    }

    @Test
    @DisplayName("sectorDisplayName повертає Unknown для null")
    void sectorDisplayName_returnsUnknown_forNull() {
        // Act
        String result = LapDataMapper.sectorDisplayName(null);

        // Assert
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("driverStatusDisplayName повертає display name для коду 4")
    void driverStatusDisplayName_returnsDisplayName_forCode4() {
        // Act
        String result = LapDataMapper.driverStatusDisplayName(4);

        // Assert
        assertThat(result).isEqualTo("On track");
    }

    @Test
    @DisplayName("resultStatusDisplayName повертає display name для коду 2")
    void resultStatusDisplayName_returnsDisplayName_forCode2() {
        // Act
        String result = LapDataMapper.resultStatusDisplayName(2);

        // Assert
        assertThat(result).isEqualTo("Active");
    }

    @Test
    @DisplayName("resultStatusDisplayName повертає Unknown для null")
    void resultStatusDisplayName_returnsUnknown_forNull() {
        // Act
        String result = LapDataMapper.resultStatusDisplayName(null);

        // Assert
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("pitLaneTimerActiveDisplayName повертає display name для коду 1")
    void pitLaneTimerActiveDisplayName_returnsDisplayName_forCode1() {
        // Act
        String result = LapDataMapper.pitLaneTimerActiveDisplayName(1);

        // Assert
        assertThat(result).isEqualTo("Active");
    }

    @Test
    @DisplayName("pitLaneTimerActiveDisplayName повертає Inactive для коду 0")
    void pitLaneTimerActiveDisplayName_returnsInactive_forCode0() {
        // Act
        String result = LapDataMapper.pitLaneTimerActiveDisplayName(0);

        // Assert
        assertThat(result).isEqualTo("Inactive");
    }
}
