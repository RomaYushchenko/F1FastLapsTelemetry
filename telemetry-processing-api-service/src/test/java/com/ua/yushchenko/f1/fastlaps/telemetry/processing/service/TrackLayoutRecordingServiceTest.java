package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.PointXYZD;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TrackRecordingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.TRACK_ID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackLayoutRecordingService")
class TrackLayoutRecordingServiceTest {

    @Mock
    private TrackLayoutRepository trackLayoutRepository;

    @Mock
    private SessionStateManager sessionStateManager;

    private TrackLayoutRecordingService service;

    @BeforeEach
    void setUp() {
        service = new TrackLayoutRecordingService(trackLayoutRepository, sessionStateManager);
    }

    @Test
    @DisplayName("onSessionStart sets WAITING_FOR_LAP_START when layout does not exist")
    void onSessionStart_setsWaitingForLapStart_whenLayoutMissing() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        when(trackLayoutRepository.existsById(TRACK_ID)).thenReturn(false);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);

        // Act
        service.onSessionStart(SESSION_UID, TRACK_ID);

        // Assert
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        assertThat(recState.getTrackId()).isEqualTo(TRACK_ID);
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
    }

    @Test
    @DisplayName("onMotionFrame starts recording when lapDistance > 0 and samples points")
    void onMotionFrame_transitionsToRecording_andSamplesPoints() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        // Act: first frame with lapDistance > 0 should switch to RECORDING
        service.onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f, 1.0f, 20.0f, 5.0f);

        // Assert
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.RECORDING);

        // Act: call several times to ensure sampling uses SAMPLE_EVERY = 5
        for (int i = 0; i < 10; i++) {
            service.onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f + i, 1.0f, 20.0f + i, 5.0f + i);
        }

        // Assert: buffer has at least one sampled point and stores XYZ + lapDistance
        List<PointXYZD> buffer = recState.getBuffer();
        assertThat(buffer).isNotEmpty();
        PointXYZD first = buffer.get(0);
        assertThat(first.x()).isNotNull();
        assertThat(first.y()).isNotNull();
        assertThat(first.z()).isNotNull();
        assertThat(first.lapDistance()).isNotNull();
    }

    @Test
    @DisplayName("onMotionFrame ignores non-player car index")
    void onMotionFrame_ignoresNonPlayerCar() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        // Act
        service.onMotionFrame(SESSION_UID, CAR_INDEX + 1, 10.0f, 1.0f, 20.0f, 5.0f);

        // Assert
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
        assertThat(recState.getBuffer()).isEmpty();
    }

    @Test
    @DisplayName("onLapComplete discards invalid lap and resets state")
    void onLapComplete_discardsInvalidLap() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.getBuffer().add(new PointXYZD(1.0f, 2.0f, 3.0f, 10.0f));

        // Act
        service.onLapComplete(SESSION_UID, CAR_INDEX, true);

        // Assert
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
        assertThat(recState.getBuffer()).isEmpty();
    }

    @Test
    @DisplayName("onLapComplete saves layout with elevation and sector boundaries for valid lap")
    void onLapComplete_savesLayout_forValidLap() throws Exception {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        runtimeState.setSector2LapDistanceStart(30.0f);
        runtimeState.setSector3LapDistanceStart(60.0f);

        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);

        // Build minimal buffer over threshold (simulate already sampled)
        for (int i = 0; i < 310; i++) {
            float dist = i;
            recState.getBuffer().add(new PointXYZD(100.0f + i, 5.0f + i, 200.0f + i, dist));
        }

        // Act
        service.onLapComplete(SESSION_UID, CAR_INDEX, false);

        // Assert
        ArgumentCaptor<TrackLayout> captor = ArgumentCaptor.forClass(TrackLayout.class);
        verify(trackLayoutRepository).save(captor.capture());
        TrackLayout saved = captor.getValue();
        assertThat(saved.getTrackId()).isEqualTo(TRACK_ID);
        assertThat(saved.getPointsJson()).isNotBlank();
        assertThat(saved.getMinElev()).isNotNull();
        assertThat(saved.getMaxElev()).isNotNull();
        assertThat(saved.getSource()).isEqualTo("RECORDED");
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(saved.getSessionUid()).isEqualTo(SESSION_UID);
        assertThat(saved.getSectorBoundariesJson()).isNotBlank();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> points = mapper.readValue(saved.getPointsJson(), new TypeReference<>() {});
        assertThat(points).hasSize(310);

        List<Map<String, Object>> boundaries = mapper.readValue(saved.getSectorBoundariesJson(), new TypeReference<>() {});
        assertThat(boundaries).hasSize(3);
        assertThat(boundaries).extracting(b -> (Integer) b.get("sector")).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("onLapComplete ignores non-player car index")
    void onLapComplete_ignoresNonPlayerCar() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.getBuffer().add(new PointXYZD(1.0f, 2.0f, 3.0f, 10.0f));

        // Act
        service.onLapComplete(SESSION_UID, CAR_INDEX + 1, false);

        // Assert
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.RECORDING);
        assertThat(recState.getBuffer()).hasSize(1);
    }

    @Test
    @DisplayName("onSessionFinished aborts recording and clears buffer")
    void onSessionFinished_abortsRecording() {
        // Arrange
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.getBuffer().add(new PointXYZD(1.0f, 2.0f, 3.0f, 10.0f));

        // Act
        service.onSessionFinished(SESSION_UID);

        // Assert
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.ABORTED);
        assertThat(recState.getBuffer()).isEmpty();
    }
}


