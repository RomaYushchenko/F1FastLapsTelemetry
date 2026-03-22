package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.LastTyreCompoundState;
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

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private SessionSummaryRepository sessionSummaryRepository;
    @Mock
    private LastTyreCompoundState lastTyreCompoundState;

    @InjectMocks
    private LeaderboardQueryService service;

    @BeforeEach
    void stubSessionSummaries() {
        lenient().when(sessionSummaryRepository.findBySessionUid(SESSION_UID)).thenReturn(List.of());
    }

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
        Lap lapP2 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(lap(), lapP2));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID))
                .thenReturn(List.of(sessionDriver()));

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPosition()).isEqualTo(1);
        assertThat(result.get(0).getCarIndex()).isEqualTo(0);
        assertThat(result.get(0).getDriverLabel()).isEqualTo("VER");
        assertThat(result.get(0).getGap()).isEqualTo("LEAD");
        assertThat(result.get(0).getLastLapGap()).isEqualTo("LEAD");
        assertThat(result.get(0).getTotalRaceTimeMs()).isEqualTo(LAP_TIME_MS);
        assertThat(result.get(0).getBestLapTimeMs()).isEqualTo(LAP_TIME_MS);
        assertThat(result.get(1).getGap()).isEqualTo("+2.50");
        assertThat(result.get(1).getLastLapGap()).isEqualTo("+2.50");
        assertThat(result.get(1).getTotalRaceTimeMs()).isEqualTo(90_000);
        assertThat(result.get(1).getBestLapTimeMs()).isEqualTo(90_000);
        assertThat(result.get(1).getPosition()).isEqualTo(2);
        assertThat(result.get(1).getDriverLabel()).isEqualTo("Car 1");
    }

    @Test
    @DisplayName("buildLeaderboard: gap — коли сумарний час P2 менший за P1 (неконсистентно)")
    void buildLeaderboard_gapEmpty_whenP2TotalRaceTimeLessThanLeader() {
        SessionRuntimeState state = runtimeStateActive();
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.updateSnapshot(0, carSnapshot());
        state.updateSnapshot(1, new SessionRuntimeState.CarSnapshot());
        Lap l0 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .build();
        Lap l1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(89_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(29_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(l0, l1));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID))
                .thenReturn(List.of(sessionDriver(), sessionDriverCar1()));

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result.get(0).getGap()).isEqualTo("LEAD");
        assertThat(result.get(1).getGap()).isEqualTo("—");
        assertThat(result.get(0).getLastLapGap()).isEqualTo("LEAD");
        assertThat(result.get(1).getLastLapGap()).isEqualTo("—");
    }

    @Test
    @DisplayName("buildLeaderboard: gap +0.00 при однаковій сумі валідних кіл")
    void buildLeaderboard_gapPlusZero_whenTiedTotalRaceTime() {
        SessionRuntimeState state = runtimeStateActive();
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.updateSnapshot(0, carSnapshot());
        state.updateSnapshot(1, new SessionRuntimeState.CarSnapshot());
        int t = 90_000;
        Lap l0 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(t)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .build();
        Lap l1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(t)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(l0, l1));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID))
                .thenReturn(List.of(sessionDriver(), sessionDriverCar1()));

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result.get(1).getGap()).isEqualTo("+0.00");
        assertThat(result.get(1).getLastLapGap()).isEqualTo("+0.00");
    }

    @Test
    @DisplayName("buildLeaderboard: lastLapGap відрізняється від cumulative gap при кількох колах")
    void buildLeaderboard_lastLapGapDiffersFromCumulative_whenMultipleLaps() {
        SessionRuntimeState state = runtimeStateActive();
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.updateSnapshot(0, carSnapshot());
        state.updateSnapshot(1, new SessionRuntimeState.CarSnapshot());
        Lap p1l1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(60_000)
                .isInvalid(false)
                .build();
        Lap p1l2 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 2)
                .lapTimeMs(70_000)
                .isInvalid(false)
                .build();
        Lap p2l1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(61_000)
                .isInvalid(false)
                .build();
        Lap p2l2 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 2)
                .lapTimeMs(71_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(p1l1, p1l2, p2l1, p2l2));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID))
                .thenReturn(List.of(sessionDriver(), sessionDriverCar1()));

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result.get(1).getGap()).isEqualTo("+2.00");
        assertThat(result.get(1).getLastLapGap()).isEqualTo("+1.00");
    }

    @Test
    @DisplayName("buildLeaderboard включає усі carIndex з позицією в межах UDP (numActiveCars не відсікає високий індекс)")
    void buildLeaderboard_includesAllCarsWithPositionWithinUdpRange() {
        SessionRuntimeState state = runtimeStateActive();
        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setName("A");
        ParticipantDataDto p1 = new ParticipantDataDto();
        p1.setCarIndex(1);
        p1.setName("B");
        state.setParticipants(List.of(p0, p1), 2);
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.setLastCarPosition(2, 3);
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of());
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of());

        List<LeaderboardEntryDto> result = service.buildLeaderboard(SESSION_UID, state);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(LeaderboardEntryDto::getCarIndex).containsExactly(0, 1, 2);
    }
}
