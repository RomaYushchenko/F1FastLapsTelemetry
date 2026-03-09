package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Map;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackLayoutService")
class TrackLayoutServiceTest {

    @Mock
    private TrackLayoutRepository trackLayoutRepository;

    @Mock
    private SessionStateManager sessionStateManager;

    @Mock
    private SessionRepository sessionRepository;

    private TrackLayoutService service;

    @BeforeEach
    void setUp() {
        service = new TrackLayoutService(trackLayoutRepository, new ObjectMapper(), sessionStateManager, sessionRepository);
    }

    @Test
    @DisplayName("getLayout повертає DTO коли layout існує")
    void getLayout_returnsDto_whenPresent() {
        // Arrange
        TrackLayout entity = trackLayout();
        when(trackLayoutRepository.findById(TRACK_LAYOUT_TRACK_ID)).thenReturn(Optional.of(entity));

        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout(TRACK_LAYOUT_TRACK_ID);

        // Assert
        assertThat(result).isPresent();
        TrackLayoutResponseDto dto = result.get();
        assertThat(dto.getTrackId()).isEqualTo((int) TRACK_LAYOUT_TRACK_ID);
        assertThat(dto.getPoints()).hasSize(4);
        assertThat(dto.getPoints().get(0).getX()).isEqualTo(100.0);
        assertThat(dto.getPoints().get(0).getY()).isEqualTo(300.0);
        assertThat(dto.getBounds()).isNotNull();
        assertThat(dto.getBounds().getMinX()).isEqualTo(100.0);
        assertThat(dto.getBounds().getMaxZ()).isEqualTo(500.0);
        verify(trackLayoutRepository).findById(TRACK_LAYOUT_TRACK_ID);
    }

    @Test
    @DisplayName("getLayout повертає empty коли trackId не знайдено")
    void getLayout_returnsEmpty_whenNotFound() {
        // Arrange
        when(trackLayoutRepository.findById((short) 99)).thenReturn(Optional.empty());

        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout((short) 99);

        // Assert
        assertThat(result).isEmpty();
        verify(trackLayoutRepository).findById((short) 99);
    }

    @Test
    @DisplayName("getLayout повертає empty коли trackId null")
    void getLayout_returnsEmpty_whenTrackIdNull() {
        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout(null);

        // Assert
        assertThat(result).isEmpty();
        // Repository is not called when trackId is null
    }

    @Test
    @DisplayName("getLayoutStatus повертає READY коли layout є в БД")
    void getLayoutStatus_ready_whenLayoutExists() {
        // Arrange
        TrackLayout entity = trackLayout();
        when(trackLayoutRepository.findById(TRACK_LAYOUT_TRACK_ID)).thenReturn(Optional.of(entity));

        // Act
        TrackLayoutStatusDto status = service.getLayoutStatus(TRACK_LAYOUT_TRACK_ID);

        // Assert
        assertThat(status.trackId()).isEqualTo(TRACK_LAYOUT_TRACK_ID);
        assertThat(status.status()).isEqualTo("READY");
        assertThat(status.pointsCollected()).isEqualTo(4);
        verify(trackLayoutRepository).findById(TRACK_LAYOUT_TRACK_ID);
    }

    @Test
    @DisplayName("getLayoutStatus повертає RECORDING коли є активна сесія з записом треку")
    void getLayoutStatus_recording_whenActiveSessionRecording() {
        // Arrange
        SessionRuntimeState runtimeState = runtimeStateActive();
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.addPoint(1.0f, 2.0f, 3.0f, 10.0f);
        recState.addPoint(2.0f, 3.0f, 4.0f, 20.0f);

        when(sessionStateManager.getAllActive()).thenReturn(Map.of(SESSION_UID, runtimeState));

        Session session = session();
        when(sessionRepository.findAllById(anyIterable())).thenReturn(List.of(session));

        // Act
        TrackLayoutStatusDto status = service.getLayoutStatus(TRACK_ID);

        // Assert
        assertThat(status.trackId()).isEqualTo(TRACK_ID);
        assertThat(status.status()).isEqualTo("RECORDING");
        assertThat(status.pointsCollected()).isEqualTo(recState.getBuffer().size());
    }
}
