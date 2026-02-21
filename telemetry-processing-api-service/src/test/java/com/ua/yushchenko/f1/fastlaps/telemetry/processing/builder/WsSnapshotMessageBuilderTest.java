package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WsSnapshotMessageBuilder")
class WsSnapshotMessageBuilderTest {

    @Test
    @DisplayName("build повертає null коли snapshot null")
    void build_returnsNull_whenSnapshotNull() {
        // Act
        WsSnapshotMessage result = WsSnapshotMessageBuilder.build(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("build мапить усі поля з CarSnapshot")
    void build_mapsAllFieldsFromCarSnapshot() {
        // Arrange
        SessionRuntimeState.CarSnapshot snapshot = carSnapshot();

        // Act
        WsSnapshotMessage msg = WsSnapshotMessageBuilder.build(snapshot);

        // Assert
        assertThat(msg).isNotNull();
        assertThat(msg.getType()).isEqualTo(WsSnapshotMessage.TYPE);
        assertThat(msg.getTimestamp()).isEqualTo(RAW_TS);
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
        assertThat(msg.getGear()).isEqualTo((int) GEAR);
        assertThat(msg.getEngineRpm()).isEqualTo(ENGINE_RPM);
        assertThat(msg.getThrottle()).isEqualTo(THROTTLE);
        assertThat(msg.getBrake()).isEqualTo(BRAKE);
        assertThat(msg.getDrs()).isTrue();
        assertThat(msg.getCurrentLap()).isEqualTo(1);
        assertThat(msg.getCurrentSector()).isEqualTo(2);
        assertThat(msg.getCurrentLapTimeMs()).isEqualTo(45_000);
        assertThat(msg.getBestLapTimeMs()).isEqualTo(43_000);
        assertThat(msg.getDeltaMs()).isEqualTo(2_000); // current - best = +2s slower
        assertThat(msg.getErsEnergyPercent()).isEqualTo(75);
        assertThat(msg.getErsDeployActive()).isFalse();
    }

    @Test
    @DisplayName("build повертає null deltaMs коли currentLapTimeMs або bestLapTimeMs null")
    void build_returnsNullDelta_whenLapTimesMissing() {
        SessionRuntimeState.CarSnapshot snapshot = carSnapshot();
        snapshot.setCurrentLapTimeMs(null);
        snapshot.setBestLapTimeMs(43_000);

        WsSnapshotMessage msg = WsSnapshotMessageBuilder.build(snapshot);

        assertThat(msg).isNotNull();
        assertThat(msg.getDeltaMs()).isNull();
    }
}
