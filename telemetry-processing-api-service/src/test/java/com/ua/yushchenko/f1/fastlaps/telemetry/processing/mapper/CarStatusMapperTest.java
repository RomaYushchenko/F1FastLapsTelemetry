package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CarStatusMapper")
class CarStatusMapperTest {

    @Test
    @DisplayName("drsStateDisplayName повертає On для коду 1")
    void drsStateDisplayName_returnsOn_forCode1() {
        // Act
        String result = CarStatusMapper.drsStateDisplayName(1);

        // Assert
        assertThat(result).isEqualTo("On");
    }

    @Test
    @DisplayName("drsStateDisplayName повертає Unknown для null")
    void drsStateDisplayName_returnsUnknown_forNull() {
        // Act
        String result = CarStatusMapper.drsStateDisplayName(null);

        // Assert
        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("ersDeployModeDisplayName повертає Hotlap для коду 2")
    void ersDeployModeDisplayName_returnsHotlap_forCode2() {
        // Act
        String result = CarStatusMapper.ersDeployModeDisplayName(2);

        // Assert
        assertThat(result).isEqualTo("Hotlap");
    }

    @Test
    @DisplayName("ersDeployModeDisplayName повертає None для коду 0")
    void ersDeployModeDisplayName_returnsNone_forCode0() {
        // Act
        String result = CarStatusMapper.ersDeployModeDisplayName(0);

        // Assert
        assertThat(result).isEqualTo("None");
    }

    @Test
    @DisplayName("faultStatusDisplayName повертає Fault для коду 1")
    void faultStatusDisplayName_returnsFault_forCode1() {
        // Act
        String result = CarStatusMapper.faultStatusDisplayName(1);

        // Assert
        assertThat(result).isEqualTo("Fault");
    }

    @Test
    @DisplayName("tractionControlDisplayName повертає Full для коду 2")
    void tractionControlDisplayName_returnsFull_forCode2() {
        // Act
        String result = CarStatusMapper.tractionControlDisplayName(2);

        // Assert
        assertThat(result).isEqualTo("Full");
    }

    @Test
    @DisplayName("fuelMixDisplayName повертає Rich для коду 2")
    void fuelMixDisplayName_returnsRich_forCode2() {
        // Act
        String result = CarStatusMapper.fuelMixDisplayName(2);

        // Assert
        assertThat(result).isEqualTo("Rich");
    }

    @Test
    @DisplayName("pitLimiterStatusDisplayName повертає On для коду 1")
    void pitLimiterStatusDisplayName_returnsOn_forCode1() {
        // Act
        String result = CarStatusMapper.pitLimiterStatusDisplayName(1);

        // Assert
        assertThat(result).isEqualTo("On");
    }
}
