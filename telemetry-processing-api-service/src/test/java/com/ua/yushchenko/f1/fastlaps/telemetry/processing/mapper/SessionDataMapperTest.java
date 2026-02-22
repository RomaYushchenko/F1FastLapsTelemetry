package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionDataMapper")
class SessionDataMapperTest {

    @Test
    @DisplayName("weatherDisplayName повертає display name для коду 0")
    void weatherDisplayName_returnsDisplayName_forCode0() {
        // Act
        String result = SessionDataMapper.weatherDisplayName(0);

        // Assert
        assertThat(result).isEqualTo("Clear");
    }

    @Test
    @DisplayName("weatherDisplayName повертає Unknown для null")
    void weatherDisplayName_returnsUnknown_forNull() {
        // Act
        String result = SessionDataMapper.weatherDisplayName(null);

        // Assert
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("formulaDisplayName повертає display name для коду 0")
    void formulaDisplayName_returnsDisplayName_forCode0() {
        // Act
        String result = SessionDataMapper.formulaDisplayName(0);

        // Assert
        assertThat(result).isEqualTo("F1 Modern");
    }

    @Test
    @DisplayName("safetyCarStatusDisplayName повертає display name для коду 2")
    void safetyCarStatusDisplayName_returnsDisplayName_forCode2() {
        // Act
        String result = SessionDataMapper.safetyCarStatusDisplayName(2);

        // Assert
        assertThat(result).isEqualTo("Virtual safety car");
    }

    @Test
    @DisplayName("sessionLengthDisplayName повертає display name для коду 7")
    void sessionLengthDisplayName_returnsDisplayName_forCode7() {
        // Act
        String result = SessionDataMapper.sessionLengthDisplayName(7);

        // Assert
        assertThat(result).isEqualTo("Full");
    }
}
