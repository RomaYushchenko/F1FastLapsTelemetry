package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionRuntimeState")
class SessionRuntimeStateTest {

    private SessionRuntimeState state;

    @BeforeEach
    void setUp() {
        state = new SessionRuntimeState(SESSION_UID);
    }

    @Test
    @DisplayName("початковий стан — INIT")
    void initialState_isInit() {
        // Act & Assert
        assertThat(state.getState()).isEqualTo(SessionState.INIT);
        assertThat(state.isActive()).isFalse();
        assertThat(state.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("transitionTo оновлює стан")
    void transitionTo_updatesState() {
        // Act
        state.transitionTo(SessionState.ACTIVE);

        // Assert
        assertThat(state.getState()).isEqualTo(SessionState.ACTIVE);
        assertThat(state.isActive()).isTrue();
    }

    @Test
    @DisplayName("lap watermark зберігає тільки більше значення")
    void lapWatermark_onlyIncreases() {
        // Act
        state.updateLapWatermark(0, 100);
        state.updateLapWatermark(0, 150);
        state.updateLapWatermark(0, 120); // idempotent: only increases

        // Assert
        assertThat(state.getLapWatermark(0)).isEqualTo(150);
    }

    @Test
    @DisplayName("telemetry та status watermarks незалежні від lap")
    void watermarksIndependentPerType() {
        state.updateLapWatermark(0, 100);
        state.updateTelemetryWatermark(0, 200);
        state.updateStatusWatermark(0, 50);

        assertThat(state.getLapWatermark(0)).isEqualTo(100);
        assertThat(state.getTelemetryWatermark(0)).isEqualTo(200);
        assertThat(state.getStatusWatermark(0)).isEqualTo(50);
    }

    @Test
    @DisplayName("getLapWatermark повертає 0 для невідомого carIndex")
    void getLapWatermark_returnsZero_forUnknownCar() {
        // Act
        int result = state.getLapWatermark(5);

        // Assert
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("updateSnapshot та getSnapshot зберігають snapshot")
    void updateSnapshot_andGetSnapshot() {
        // Arrange
        SessionRuntimeState.CarSnapshot snapshot = carSnapshot();

        // Act
        state.updateSnapshot(0, snapshot);

        // Assert
        assertThat(state.getSnapshot(0)).isSameAs(snapshot);
    }

    @Test
    @DisplayName("getLatestSnapshot повертає null коли snapshots порожні")
    void getLatestSnapshot_returnsNull_whenEmpty() {
        // Act
        WsSnapshotMessage result = state.getLatestSnapshot();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLatestSnapshot повертає повідомлення коли є snapshot")
    void getLatestSnapshot_returnsMessage_whenSnapshotPresent() {
        // Arrange
        state.updateSnapshot(0, carSnapshot());

        // Act
        WsSnapshotMessage msg = state.getLatestSnapshot();

        // Assert
        assertThat(msg).isNotNull();
        assertThat(msg.getType()).isEqualTo(WsSnapshotMessage.TYPE);
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
    }

    @Test
    @DisplayName("getLatestSnapshot повертає snapshot гравця при carIndex != 0 (Practice)")
    void getLatestSnapshot_returnsPlayerSnapshot_whenCarIndexNotZero() {
        // In Practice player can be carIndex 5; ingest sends only player car
        SessionRuntimeState.CarSnapshot playerSnapshot = carSnapshot();
        state.updateSnapshot(5, playerSnapshot);

        WsSnapshotMessage msg = state.getLatestSnapshot();

        assertThat(msg).isNotNull();
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
    }

    @Test
    @DisplayName("isTerminal true після переходу в TERMINAL")
    void isTerminal_afterTransitionToTerminal() {
        // Act
        state.transitionTo(SessionState.TERMINAL);

        // Assert
        assertThat(state.isTerminal()).isTrue();
        assertThat(state.isActive()).isFalse();
    }
}
