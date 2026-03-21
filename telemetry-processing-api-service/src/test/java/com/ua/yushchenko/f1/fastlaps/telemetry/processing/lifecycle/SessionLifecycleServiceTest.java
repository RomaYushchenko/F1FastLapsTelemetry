package com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation.LapAggregator;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutRecordingService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.EndReason;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.LiveDataBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.session;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SessionLifecycleService")
@ExtendWith(MockitoExtension.class)
class SessionLifecycleServiceTest {

    private SessionStateManager stateManager;

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionFinishingPositionRepository finishingPositionRepository;
    @Mock
    private SessionDriverRepository sessionDriverRepository;
    @Mock
    private SessionPersistenceService sessionPersistenceService;
    @Mock
    private LapAggregator lapAggregator;
    @Mock
    private LiveDataBroadcaster liveDataBroadcaster;
    @Mock
    private TrackLayoutRecordingService trackLayoutRecordingService;

    private SessionLifecycleService sessionLifecycleService;

    @BeforeEach
    void setUp() {
        stateManager = new SessionStateManager();
        sessionLifecycleService = new SessionLifecycleService(
                stateManager,
                sessionRepository,
                finishingPositionRepository,
                sessionDriverRepository,
                sessionPersistenceService,
                lapAggregator,
                liveDataBroadcaster,
                trackLayoutRecordingService
        );
    }

    @Test
    @DisplayName("onSessionEnded фіналізує сесію коли стан був ACTIVE")
    void onSessionEnded_finalizes_whenWasActive() {
        // Arrange
        stateManager.getOrCreate(SESSION_UID).transitionTo(SessionState.ACTIVE);
        when(sessionRepository.findById(SESSION_UID)).thenReturn(Optional.of(session()));

        // Act
        sessionLifecycleService.onSessionEnded(SESSION_UID, null, EndReason.EVENT_SEND);

        // Assert
        verify(lapAggregator).finalizeAllLaps(SESSION_UID);
        verify(liveDataBroadcaster).notifySessionEnded(SESSION_UID, EndReason.EVENT_SEND.name());
        verify(trackLayoutRecordingService).onSessionFinished(SESSION_UID);
    }

    @Test
    @DisplayName("onSessionEnded нічого не фіналізує коли сесії немає в stateManager")
    void onSessionEnded_skipsFinalize_whenStateMissing() {
        // Act
        sessionLifecycleService.onSessionEnded(SESSION_UID, null, EndReason.EVENT_SEND);

        // Assert
        verify(lapAggregator, never()).finalizeAllLaps(SESSION_UID);
    }

    @Test
    @DisplayName("onSessionEnded нічого не фіналізує коли стан не ACTIVE")
    void onSessionEnded_skipsFinalize_whenNotActive() {
        // Arrange
        stateManager.getOrCreate(SESSION_UID).transitionTo(SessionState.INIT);

        // Act
        sessionLifecycleService.onSessionEnded(SESSION_UID, null, EndReason.EVENT_SEND);

        // Assert
        verify(lapAggregator, never()).finalizeAllLaps(SESSION_UID);
    }
}
