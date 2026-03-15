package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.model.SectorBoundary;
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

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.TRACK_ID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @DisplayName("onMotionFrame starts recording when lap 1 and lapDistance > 0 and samples points")
    void onMotionFrame_transitionsToRecording_andSamplesPoints() {
        // Arrange: snapshot with currentLap=1 so we start from first lap only
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        SessionRuntimeState.CarSnapshot snapshot = new SessionRuntimeState.CarSnapshot();
        snapshot.setCurrentLap(1);
        runtimeState.updateSnapshot(CAR_INDEX, snapshot);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        // Act: first frame with lap 1 and lapDistance > 0 should switch to RECORDING
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
    @DisplayName("onMotionFrame does not start when currentLap is null (LapData not yet processed)")
    void onMotionFrame_doesNotStartWhenCurrentLapNull() {
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        service.onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f, 1.0f, 20.0f, 5.0f);

        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
    }

    @Test
    @DisplayName("onMotionFrame does not start recording when currentLap is 0 (formation lap)")
    void onMotionFrame_doesNotStartWhenLap0() {
        // Arrange: lap 0 = before first racing lap (formation lap)
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        SessionRuntimeState.CarSnapshot snapshot = new SessionRuntimeState.CarSnapshot();
        snapshot.setCurrentLap(0);
        runtimeState.updateSnapshot(CAR_INDEX, snapshot);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        service.onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f, 1.0f, 20.0f, 5.0f);

        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
        assertThat(recState.getBuffer()).isEmpty();
    }

    @Test
    @DisplayName("onMotionFrame does not start recording when currentLap is 2 (second lap)")
    void onMotionFrame_doesNotStartWhenLap2() {
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        SessionRuntimeState.CarSnapshot snapshot = new SessionRuntimeState.CarSnapshot();
        snapshot.setCurrentLap(2);
        runtimeState.updateSnapshot(CAR_INDEX, snapshot);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.WAITING_FOR_LAP_START);

        service.onMotionFrame(SESSION_UID, CAR_INDEX, 10.0f, 1.0f, 20.0f, 100.0f);

        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.WAITING_FOR_LAP_START);
        assertThat(recState.getBuffer()).isEmpty();
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
    @DisplayName("onLapComplete saves track even when lap invalid (layout geometry still valid)")
    void onLapComplete_savesTrackWhenLapInvalid() {
        // Arrange: enough points; lap invalid (e.g. cut track) but we still save layout
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        for (int i = 0; i < 310; i++) {
            recState.getBuffer().add(new PointXYZD(1.0f + i, 2.0f, 3.0f, 10.0f + i));
        }

        // Act
        service.onLapComplete(SESSION_UID, CAR_INDEX, true);

        // Assert: track is saved (invalid lap still has valid track geometry)
        ArgumentCaptor<TrackLayout> captor = ArgumentCaptor.forClass(TrackLayout.class);
        verify(trackLayoutRepository).save(captor.capture());
        assertThat(captor.getValue().getTrackId()).isEqualTo(TRACK_ID);
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.DONE);
    }

    @Test
    @DisplayName("onLapComplete defers save when points < threshold (cross-topic ordering)")
    void onLapComplete_defersWhenPointsBelowThreshold() {
        // Arrange: lap complete arrived before enough motion (e.g. different Kafka topics)
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);
        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.getBuffer().add(new PointXYZD(1.0f, 2.0f, 3.0f, 10.0f));

        // Act
        service.onLapComplete(SESSION_UID, CAR_INDEX, false);

        // Assert: not reset, pending set so save will run when motion adds enough points
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.RECORDING);
        assertThat(recState.getBuffer()).hasSize(1);
        assertThat(recState.isPendingLapComplete()).isTrue();
        assertThat(recState.isPendingLapInvalid()).isFalse();
    }

    @Test
    @DisplayName("onMotionFrame does not save without lap complete (criterion is lap only)")
    void onMotionFrame_doesNotSaveWithoutLapComplete() {
        // Arrange: RECORDING with many points, no lap complete signal
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);

        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.setPendingLapComplete(false);
        for (int i = 0; i < 200; i++) {
            recState.getBuffer().add(new PointXYZD(100.0f + i, 5.0f, 200.0f + i, (float) i));
        }

        // Act: more motion frames (no lap complete)
        for (int i = 0; i < 10; i++) {
            service.onMotionFrame(SESSION_UID, CAR_INDEX, 399.0f, 5.0f, 499.0f, 299.0f + i);
        }

        // Assert: not saved (criterion is lap completion, not point count)
        verify(trackLayoutRepository, never()).save(any());
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.RECORDING);
    }

    @Test
    @DisplayName("onMotionFrame triggers deferred save when buffer reaches min points after pending lap complete")
    void onMotionFrame_triggersDeferredSaveWhenPendingAndEnoughPoints() {
        // Arrange: lap complete was deferred (too few points); motion catches up to min (10)
        SessionRuntimeState runtimeState = new SessionRuntimeState(SESSION_UID);
        runtimeState.setPlayerCarIndex(CAR_INDEX);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(runtimeState);

        TrackRecordingState recState = runtimeState.getTrackRecordingState();
        recState.setTrackId(TRACK_ID);
        recState.setStatus(TrackRecordingState.Status.RECORDING);
        recState.setPendingLapComplete(true);
        recState.setPendingLapInvalid(false);
        for (int i = 0; i < 9; i++) {
            recState.getBuffer().add(new PointXYZD(100.0f + i, 5.0f, 200.0f + i, (float) i));
        }

        // Act: one more sampled motion frame reaches 10 and triggers deferred save (every 5th frame samples)
        for (int i = 0; i < 5; i++) {
            service.onMotionFrame(SESSION_UID, CAR_INDEX, 399.0f, 5.0f, 499.0f, 299.0f + i);
        }

        // Assert
        ArgumentCaptor<TrackLayout> captor = ArgumentCaptor.forClass(TrackLayout.class);
        verify(trackLayoutRepository).save(captor.capture());
        assertThat(captor.getValue().getTrackId()).isEqualTo(TRACK_ID);
        assertThat(recState.getStatus()).isEqualTo(TrackRecordingState.Status.DONE);
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
        assertThat(saved.getPoints()).isNotEmpty().hasSize(310);
        assertThat(saved.getMinElev()).isNotNull();
        assertThat(saved.getMaxElev()).isNotNull();
        assertThat(saved.getSource()).isEqualTo("RECORDED");
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(saved.getSessionUid()).isEqualTo(SESSION_UID);
        assertThat(saved.getSectorBoundaries()).isNotEmpty().hasSize(3);
        assertThat(saved.getSectorBoundaries()).extracting(SectorBoundary::sector).containsExactly(1, 2, 3);
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


