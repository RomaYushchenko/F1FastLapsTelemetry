package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListResult;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionQueryService")
class SessionQueryServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private SessionFinishingPositionRepository finishingPositionRepository;
    @Mock
    private SessionStateManager stateManager;
    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private SessionSearchResolver sessionSearchResolver;
    @Spy
    private SessionMapper sessionMapper = new SessionMapper();

    @InjectMocks
    private SessionQueryService service;

    @Test
    @DisplayName("listSessions повертає змаплені DTO")
    void listSessions_returnsMappedDtos() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(sessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session), PageRequest.of(0, 50), 1L));
        when(sessionRepository.count(any(Specification.class))).thenReturn(1L);
        when(stateManager.get(SESSION_UID)).thenReturn(state);
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(any())).thenReturn(Collections.emptyList());

        // Act
        SessionListResult result = service.listSessions(SessionListFilter.builder().offset(0).limit(50).build());

        // Assert
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(result.getList().get(0).getSessionType()).isEqualTo("RACE");
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(sessionRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 50)));
        verify(sessionRepository).count(any(Specification.class));
    }

    @Test
    @DisplayName("listSessions обмежує limit до 100")
    void listSessions_clampsLimitTo100() {
        // Arrange
        when(sessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0L));
        when(sessionRepository.count(any(Specification.class))).thenReturn(0L);

        // Act
        service.listSessions(SessionListFilter.builder().offset(0).limit(200).build());

        // Assert
        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(sessionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("getSession повертає DTO коли сесія знайдена")
    void getSession_returnsDto_whenFound() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(stateManager.get(SESSION_UID)).thenReturn(state);
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(any())).thenReturn(Collections.emptyList());

        // Act
        SessionDto dto = service.getSession(SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(dto.getState()).isEqualTo(com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState.ACTIVE);
    }

    @Test
    @DisplayName("getSession кидає виняток коли id дорівнює 'active'")
    void getSession_throws_whenIdIsActive() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSession("active"))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Use GET /api/sessions/active");
    }

    @Test
    @DisplayName("getSession кидає виняток коли id порожній")
    void getSession_throws_whenIdBlank() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSession("   "))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session id is required");
    }

    @Test
    @DisplayName("getActiveSession повертає empty коли немає активних")
    void getActiveSession_returnsEmpty_whenNoActive() {
        // Arrange
        when(stateManager.getAllActive()).thenReturn(Map.of());

        // Act
        var result = service.getActiveSession();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getActiveSession повертає DTO коли є одна активна сесія")
    void getActiveSession_returnsDto_whenOneActive() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(stateManager.getAllActive()).thenReturn(Map.of(SESSION_UID, state));
        when(sessionRepository.findById(SESSION_UID)).thenReturn(Optional.of(session));
        when(stateManager.get(SESSION_UID)).thenReturn(state);
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(any())).thenReturn(Collections.emptyList());

        // Act
        Optional<SessionDto> result = service.getActiveSession();

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getActiveSession повертає empty коли є лише сесія в ENDING (не показувати минулу кваліфікацію замість гонки)")
    void getActiveSession_returnsEmpty_whenOnlyEnding() {
        // Arrange: session in ENDING (e.g. qualification just finished) must not be returned as "active"
        SessionRuntimeState state = new SessionRuntimeState(SESSION_UID);
        state.transitionTo(SessionState.ENDING);
        when(stateManager.getAllActive()).thenReturn(Map.of(SESSION_UID, state));

        // Act
        Optional<SessionDto> result = service.getActiveSession();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopicIdForSession повертає empty коли sessionUid null")
    void getTopicIdForSession_returnsEmpty_whenSessionUidNull() {
        // Act
        var result = service.getTopicIdForSession(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopicIdForSession повертає empty коли сесія не знайдена")
    void getTopicIdForSession_returnsEmpty_whenSessionNotFound() {
        // Arrange
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        var result = service.getTopicIdForSession(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopicIdForSession повертає publicId коли сесія знайдена")
    void getTopicIdForSession_returnsPublicId_whenFound() {
        // Arrange
        Session session = session();
        when(sessionRepository.findById(SESSION_UID)).thenReturn(Optional.of(session));

        // Act
        var result = service.getTopicIdForSession(SESSION_UID);

        // Assert
        assertThat(result).contains(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getSession виводить playerCarIndex з laps коли в сесії null (старі сесії)")
    void getSession_infersPlayerCarIndex_fromLaps_whenNull() {
        // Arrange: session without player_car_index (e.g. created before we added the column)
        Session session = session();
        session.setPlayerCarIndex(null);
        Lap lap = Lap.builder().sessionUid(SESSION_UID).carIndex((short) 5).lapNumber((short) 1).build();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(stateManager.get(SESSION_UID)).thenReturn(runtimeStateActive());
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(lap));

        // Act
        SessionDto dto = service.getSession(SESSION_PUBLIC_ID_STR);

        // Assert: UI will use this to request laps/summary for car 5
        assertThat(dto.getPlayerCarIndex()).isEqualTo(5);
    }
}
