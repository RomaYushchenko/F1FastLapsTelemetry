package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PitStopDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.PitStopMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PitStopQueryService")
class PitStopQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private TyreWearPerLapRepository tyreWearPerLapRepository;
    @Spy
    private PitStopMapper pitStopMapper = new PitStopMapper();

    @InjectMocks
    private PitStopQueryService service;

    @Test
    @DisplayName("getPitStops повертає один піт при зміні compound між колами 1 і 2")
    void getPitStops_returnsOnePit_whenCompoundChangeBetweenLap1And2() {
        // Arrange
        Session session = session();
        List<Lap> laps = lapsForPitScenario();
        List<TyreWearPerLap> tyreWear = tyreWearTwoLapsOnePit();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(laps);
        when(tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(tyreWear);

        // Act
        List<PitStopDto> result = service.getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        PitStopDto pit = result.get(0);
        assertThat(pit.getLapNumber()).isEqualTo(2);
        assertThat(pit.getInLapTimeMs()).isEqualTo(92_500);
        assertThat(pit.getOutLapTimeMs()).isEqualTo(95_200);
        assertThat(pit.getCompoundIn()).isEqualTo(18);
        assertThat(pit.getCompoundOut()).isEqualTo(16);
        verify(sessionResolveService).getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getPitStops повертає порожній список коли немає зміни compound")
    void getPitStops_returnsEmpty_whenNoCompoundChange() {
        // Arrange
        Session session = session();
        List<Lap> laps = lapsForPitScenario();
        List<TyreWearPerLap> sameCompound = List.of(
                tyreWearPerLap(),
                TyreWearPerLap.builder()
                        .sessionUid(SESSION_UID)
                        .carIndex(CAR_INDEX)
                        .lapNumber((short) 2)
                        .wearFL(0.05f)
                        .wearFR(0.05f)
                        .wearRL(0.06f)
                        .wearRR(0.06f)
                        .compound(COMPOUND_18)
                        .build()
        );
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(laps);
        when(tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(sameCompound);

        // Act
        List<PitStopDto> result = service.getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPitStops кидає SessionNotFoundException коли сесія не знайдена")
    void getPitStops_throws_whenSessionNotFound() {
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR))
                .thenThrow(new SessionNotFoundException("Session not found: " + SESSION_PUBLIC_ID_STR));

        assertThatThrownBy(() -> service.getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX))
                .isInstanceOf(SessionNotFoundException.class);
    }
}
