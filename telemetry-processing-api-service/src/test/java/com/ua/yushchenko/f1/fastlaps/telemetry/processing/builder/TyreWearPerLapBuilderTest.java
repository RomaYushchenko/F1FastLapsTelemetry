package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TyreWearPerLapBuilder")
class TyreWearPerLapBuilderTest {

    @Test
    @DisplayName("fromSnapshot повертає null коли snapshot null")
    void fromSnapshot_returnsNull_whenSnapshotNull() {
        // Act
        TyreWearPerLap result = TyreWearPerLapBuilder.fromSnapshot(SESSION_UID, CAR_INDEX, LAP_NUMBER, null, null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromSnapshot будує entity з усіма значеннями зносу")
    void fromSnapshot_buildsEntityWithAllWearValues() {
        // Arrange
        TyreWearSnapshot snapshot = tyreWearSnapshot();

        // Act
        Short compound = (short) 18;
        TyreWearPerLap entity = TyreWearPerLapBuilder.fromSnapshot(SESSION_UID, CAR_INDEX, LAP_NUMBER, snapshot, compound);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getSessionUid()).isEqualTo(SESSION_UID);
        assertThat(entity.getCarIndex()).isEqualTo(CAR_INDEX);
        assertThat(entity.getLapNumber()).isEqualTo(LAP_NUMBER);
        assertThat(entity.getWearFL()).isEqualTo(WEAR_FL);
        assertThat(entity.getWearFR()).isEqualTo(WEAR_FR);
        assertThat(entity.getWearRL()).isEqualTo(WEAR_RL);
        assertThat(entity.getWearRR()).isEqualTo(WEAR_RR);
        assertThat(entity.getCompound()).isEqualTo(compound);
    }
}
