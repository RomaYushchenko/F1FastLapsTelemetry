package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionRaceOverviewDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.CAR_INDEX_1;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_PUBLIC_ID_STR;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RaceOverviewQueryService")
class RaceOverviewQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private SessionFinishingPositionRepository finishingPositionRepository;
    @Mock
    private SessionDriverRepository sessionDriverRepository;
    @Mock
    private TyreWearPerLapRepository tyreWearPerLapRepository;
    @Mock
    private SessionSummaryRepository sessionSummaryRepository;

    private RaceOverviewQueryService service;

    @BeforeEach
    void setUp() {
        service = new RaceOverviewQueryService(
                sessionResolveService,
                lapRepository,
                finishingPositionRepository,
                sessionDriverRepository,
                tyreWearPerLapRepository,
                sessionSummaryRepository
        );
        when(sessionSummaryRepository.findBySessionUid(anyLong())).thenReturn(List.of());
    }

    @Test
    @DisplayName("getLeaderboardForSession повертає LEAD для P1 та cumulative gap для P2")
    void getLeaderboardForSession_returnsEntriesOrderedByFinishing() {
        Session s = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(s);
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .finishingPosition(1)
                                .tyreCompound("S")
                                .build(),
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX_1)
                                .finishingPosition(2)
                                .tyreCompound("M")
                                .build()
                ));
        Lap lap0L1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .positionAtLapStart(1)
                .build();
        Lap lap1L1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(91_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_500)
                .sector3TimeMs(30_500)
                .isInvalid(false)
                .positionAtLapStart(2)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(lap0L1, lap1L1));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of());
        when(tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of());

        List<LeaderboardEntryDto> entries = service.getLeaderboardForSession(SESSION_PUBLIC_ID_STR);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getPosition()).isEqualTo(1);
        assertThat(entries.get(0).getGap()).isEqualTo("LEAD");
        assertThat(entries.get(0).getLastLapGap()).isEqualTo("LEAD");
        assertThat(entries.get(0).getCompound()).isEqualTo("S");
        assertThat(entries.get(0).getTotalRaceTimeMs()).isEqualTo(90_000);
        assertThat(entries.get(0).getBestLapTimeMs()).isEqualTo(90_000);
        assertThat(entries.get(1).getGap()).isEqualTo("+1.00");
        assertThat(entries.get(1).getLastLapGap()).isEqualTo("+1.00");
        assertThat(entries.get(1).getTotalRaceTimeMs()).isEqualTo(91_000);
        assertThat(entries.get(1).getBestLapTimeMs()).isEqualTo(91_000);
        assertThat(entries.get(1).getCompound()).isEqualTo("M");
    }

    @Test
    @DisplayName("getRaceOverview будує рядки графіків по колах")
    void getRaceOverview_buildsChartRows() {
        Session s = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(s);
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .finishingPosition(1)
                                .build(),
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX_1)
                                .finishingPosition(2)
                                .build()
                ));
        Lap lap0L1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .positionAtLapStart(1)
                .build();
        Lap lap1L1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .positionAtLapStart(2)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(lap0L1, lap1L1));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of(
                SessionDriver.builder().sessionUid(SESSION_UID).carIndex(CAR_INDEX).driverLabel("AAA").build(),
                SessionDriver.builder().sessionUid(SESSION_UID).carIndex(CAR_INDEX_1).driverLabel("BBB").build()
        ));
        when(tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of());

        SessionRaceOverviewDto dto = service.getRaceOverview(SESSION_PUBLIC_ID_STR);

        assertThat(dto.getDrivers()).hasSize(2);
        assertThat(dto.getPositionChartRows()).hasSize(1);
        assertThat(dto.getPositionChartRows().get(0).getLapNumber()).isEqualTo(1);
        assertThat(dto.getPositionChartRows().get(0).getValues()).containsExactly(1.0, 2.0);
        assertThat(dto.getGapChartRows().get(0).getValues()).containsExactly(0.0, 0.0);
    }

    @Test
    @DisplayName("getLeaderboardForSession: коли сумарний час P2 менший за P1 gap показує —")
    void getLeaderboardForSession_gapEmpty_whenP2TotalLessThanLeader() {
        Session s = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(s);
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .finishingPosition(1)
                                .build(),
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX_1)
                                .finishingPosition(2)
                                .build()
                ));
        Lap lapP1 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .isInvalid(false)
                .build();
        Lap lapP2short = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(30_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(lapP1, lapP2short));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of());
        when(tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of());

        List<LeaderboardEntryDto> entries = service.getLeaderboardForSession(SESSION_PUBLIC_ID_STR);

        assertThat(entries.get(1).getGap()).isEqualTo("—");
        assertThat(entries.get(1).getLastLapGap()).isEqualTo("—");
    }

    @Test
    @DisplayName("getLeaderboardForSession: при однаковій сумі кіл gap для P2 +0.00")
    void getLeaderboardForSession_gapPlusZero_whenTiedTotalRaceTime() {
        Session s = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(s);
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .finishingPosition(1)
                                .build(),
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX_1)
                                .finishingPosition(2)
                                .build()
                ));
        Lap lapA = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .isInvalid(false)
                .build();
        Lap lapB = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX_1)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(lapA, lapB));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of());
        when(tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of());

        List<LeaderboardEntryDto> entries = service.getLeaderboardForSession(SESSION_PUBLIC_ID_STR);

        assertThat(entries.get(1).getGap()).isEqualTo("+0.00");
        assertThat(entries.get(1).getLastLapGap()).isEqualTo("+0.00");
    }

    @Test
    @DisplayName("getLeaderboardForSession: compound з tyre_wear коли finishing без шини")
    void getLeaderboardForSession_compoundFromTyreWear_whenFinishingTyreMissing() {
        Session s = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(s);
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(
                        SessionFinishingPosition.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .finishingPosition(1)
                                .tyreCompound(null)
                                .build()
                ));
        Lap lap0 = Lap.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .lapNumber((short) 1)
                .lapTimeMs(90_000)
                .isInvalid(false)
                .build();
        when(lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID)).thenReturn(List.of(lap0));
        when(sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(SESSION_UID)).thenReturn(List.of());
        when(tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(SESSION_UID))
                .thenReturn(List.of(
                        TyreWearPerLap.builder()
                                .sessionUid(SESSION_UID)
                                .carIndex(CAR_INDEX)
                                .lapNumber((short) 1)
                                .compound((short) 18)
                                .build()
                ));

        List<LeaderboardEntryDto> entries = service.getLeaderboardForSession(SESSION_PUBLIC_ID_STR);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getCompound()).isEqualTo("M");
    }
}
