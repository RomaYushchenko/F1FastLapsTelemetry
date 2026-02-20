package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LapBuilder")
class LapBuilderTest {

    @Test
    @DisplayName("build створює Lap з усіма заданими полями")
    void build_createsLapWithAllFields() {
        // Act
        Lap lap = LapBuilder.build(
                SESSION_UID,
                CAR_INDEX,
                LAP_NUMBER,
                LAP_TIME_MS,
                SECTOR1_MS,
                SECTOR2_MS,
                SECTOR3_MS,
                false,
                (short) 0,
                LAP_ENDED_AT
        );

        // Assert
        assertThat(lap).isNotNull();
        assertThat(lap.getSessionUid()).isEqualTo(SESSION_UID);
        assertThat(lap.getCarIndex()).isEqualTo(CAR_INDEX);
        assertThat(lap.getLapNumber()).isEqualTo(LAP_NUMBER);
        assertThat(lap.getLapTimeMs()).isEqualTo(LAP_TIME_MS);
        assertThat(lap.getSector1TimeMs()).isEqualTo(SECTOR1_MS);
        assertThat(lap.getSector2TimeMs()).isEqualTo(SECTOR2_MS);
        assertThat(lap.getSector3TimeMs()).isEqualTo(SECTOR3_MS);
        assertThat(lap.getIsInvalid()).isFalse();
        assertThat(lap.getPenaltiesSeconds()).isEqualTo((short) 0);
        assertThat(lap.getEndedAt()).isEqualTo(LAP_ENDED_AT);
    }

    @Test
    @DisplayName("build створює invalid lap з пенальті")
    void build_createsInvalidLapWithPenalties() {
        // Act
        Lap lap = LapBuilder.build(
                SESSION_UID,
                CAR_INDEX,
                (short) 2,
                90_000,
                29_000,
                31_000,
                30_000,
                true,
                (short) 5,
                LAP_ENDED_AT
        );

        // Assert
        assertThat(lap.getIsInvalid()).isTrue();
        assertThat(lap.getPenaltiesSeconds()).isEqualTo((short) 5);
        assertThat(lap.getLapNumber()).isEqualTo((short) 2);
    }
}
