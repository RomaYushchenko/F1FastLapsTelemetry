package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.StintDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.StintMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
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
@DisplayName("StintQueryService")
class StintQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private TyreWearPerLapRepository tyreWearPerLapRepository;
    @Spy
    private StintMapper stintMapper = new StintMapper();

    @InjectMocks
    private StintQueryService service;

    @Test
    @DisplayName("getStints повертає два стінти для сценарію 1-5 compound 18, 6-10 compound 16")
    void getStints_returnsTwoStints_forTwoCompoundRanges() {
        // Arrange
        Session session = session();
        List<Lap> laps = lapsForStintScenario();
        List<TyreWearPerLap> tyreWear = tyreWearTwoStints();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(laps);
        when(tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(tyreWear);

        // Act
        List<StintDto> result = service.getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStintIndex()).isEqualTo(1);
        assertThat(result.get(0).getCompound()).isEqualTo(18);
        assertThat(result.get(0).getStartLap()).isEqualTo(1);
        assertThat(result.get(0).getLapCount()).isEqualTo(5);
        assertThat(result.get(1).getStintIndex()).isEqualTo(2);
        assertThat(result.get(1).getCompound()).isEqualTo(16);
        assertThat(result.get(1).getStartLap()).isEqualTo(6);
        assertThat(result.get(1).getLapCount()).isEqualTo(5);
        verify(sessionResolveService).getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getStints повертає порожній список коли немає laps")
    void getStints_returnsEmpty_whenNoLaps() {
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(List.of());
        when(tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX)).thenReturn(List.of(tyreWearPerLap()));

        List<StintDto> result = service.getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getStints кидає SessionNotFoundException коли сесія не знайдена")
    void getStints_throws_whenSessionNotFound() {
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR))
                .thenThrow(new SessionNotFoundException("Session not found: " + SESSION_PUBLIC_ID_STR));

        assertThatThrownBy(() -> service.getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX))
                .isInstanceOf(SessionNotFoundException.class);
    }
}
