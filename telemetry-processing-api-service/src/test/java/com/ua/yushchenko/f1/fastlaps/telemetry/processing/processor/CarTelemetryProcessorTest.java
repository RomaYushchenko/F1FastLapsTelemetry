package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("CarTelemetryProcessor")
@ExtendWith(MockitoExtension.class)
class CarTelemetryProcessorTest {

    @Mock
    private SessionStateManager stateManager;
    @Mock
    private SessionRuntimeState state;
    @Mock
    private com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.RawTelemetryWriter rawTelemetryWriter;

    private CarTelemetryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CarTelemetryProcessor(stateManager, rawTelemetryWriter);
        when(stateManager.getOrCreate(SESSION_UID)).thenReturn(state);
        when(state.getTelemetryWatermark(CAR_INDEX)).thenReturn(0);
        when(state.getSnapshot(CAR_INDEX)).thenReturn(null);
        when(state.isActive()).thenReturn(false);
    }

    @Test
    @DisplayName("process встановлює snapshot.drs true коли telemetry.drs = 1")
    void process_setsSnapshotDrsTrue_whenTelemetryDrs1() {
        // Arrange
        CarTelemetryDto telemetry = CarTelemetryDto.builder()
                .speedKph((int) SPEED_KPH)
                .gear((int) GEAR)
                .engineRpm(ENGINE_RPM)
                .throttle(THROTTLE)
                .brake(BRAKE)
                .drs(1)
                .build();

        // Act
        processor.process(SESSION_UID, CAR_INDEX, FRAME_ID, telemetry, 0f);

        // Assert
        ArgumentCaptor<SessionRuntimeState.CarSnapshot> captor = ArgumentCaptor.forClass(SessionRuntimeState.CarSnapshot.class);
        verify(state).updateSnapshot(anyInt(), captor.capture());
        assertThat(captor.getValue().getDrs()).isTrue();
    }

    @Test
    @DisplayName("process встановлює snapshot.drs false коли telemetry.drs = 0")
    void process_setsSnapshotDrsFalse_whenTelemetryDrs0() {
        // Arrange
        CarTelemetryDto telemetry = CarTelemetryDto.builder()
                .speedKph((int) SPEED_KPH)
                .gear((int) GEAR)
                .engineRpm(ENGINE_RPM)
                .throttle(THROTTLE)
                .brake(BRAKE)
                .drs(0)
                .build();

        // Act
        processor.process(SESSION_UID, CAR_INDEX, FRAME_ID, telemetry, 0f);

        // Assert
        ArgumentCaptor<SessionRuntimeState.CarSnapshot> captor = ArgumentCaptor.forClass(SessionRuntimeState.CarSnapshot.class);
        verify(state).updateSnapshot(anyInt(), captor.capture());
        assertThat(captor.getValue().getDrs()).isFalse();
    }
}
