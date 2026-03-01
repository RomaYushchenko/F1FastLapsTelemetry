package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardQueryService")
class LeaderboardQueryServiceTest {

    @Mock
    private SessionStateManager stateManager;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private SessionDriverRepository sessionDriverRepository;

    @InjectMocks
    private LeaderboardQueryService service;

    @Test
    @DisplayName("getLeaderboardForActiveSession повертає порожній список коли немає активної сесії")
    void getLeaderboardForActiveSession_returnsEmpty_whenNoActive() {
        when(stateManager.getAllActive()).thenReturn(Map.of());

        List<LeaderboardEntryDto> result = service.getLeaderboardForActiveSession();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLeaderboardForActiveSession повертає порожній список коли позиції порожні")
    void getLeaderboardForActiveSession_returnsEmpty_whenNoPositions() {
        SessionRuntimeState state = runtimeStateActive();
        when(stateManager.getAllActive()).thenReturn(Map.of(SESSION_UID, state));

        List<LeaderboardEntryDto> result = service.getLeaderboardForActiveSession();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("buildLeaderboard повертає записи з позицією та driverLabel fallback")
    void buildLeaderboard_returnsEntries_withPositionAndDriverFallback() {
        SessionRuntimeState state = runtimeStateActive();
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.updateSnapshot(0, carSnapshot());
        state.updateSnapshot(1, new SessionRuntimeState.CarSnapshot());
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(lap()));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID))
                .thenReturn(List.of(sessionDriver()));

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPosition()).isEqualTo(1);
        assertThat(result.get(0).getCarIndex()).isEqualTo(0);
        assertThat(result.get(0).getDriverLabel()).isEqualTo("VER");
        assertThat(result.get(0).getGap()).isEqualTo("LEAD");
        assertThat(result.get(1).getPosition()).isEqualTo(2);
        assertThat(result.get(1).getDriverLabel()).isEqualTo("Car 1");
    }
}
