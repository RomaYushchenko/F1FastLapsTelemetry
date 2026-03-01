package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ComparisonResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComparisonQueryService")
class ComparisonQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private LapQueryService lapQueryService;
    @Mock
    private SessionSummaryQueryService sessionSummaryQueryService;

    @InjectMocks
    private ComparisonQueryService service;

    private static SessionSummaryDto summaryWithBestLap(int totalLaps, Integer bestLapNumber) {
        return SessionSummaryDto.builder()
                .totalLaps(totalLaps)
                .bestLapNumber(bestLapNumber)
                .bestLapTimeMs(87_200)
                .bestSector1Ms(27_900)
                .bestSector2Ms(30_400)
                .bestSector3Ms(28_900)
                .build();
    }

    @Test
    @DisplayName("getComparison кидає 400 коли carIndexA дорівнює carIndexB")
    void getComparison_throws_whenSameCar() {
        // Act & Assert
        assertThatThrownBy(() -> service.getComparison(SESSION_PUBLIC_ID_STR, 0, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    @DisplayName("getComparison кидає коли carIndexA null")
    void getComparison_throws_whenCarIndexANull() {
        assertThatThrownBy(() -> service.getComparison(SESSION_PUBLIC_ID_STR, null, 1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("getComparison кидає коли carIndexB null")
    void getComparison_throws_whenCarIndexBNull() {
        assertThatThrownBy(() -> service.getComparison(SESSION_PUBLIC_ID_STR, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("getComparison повертає DTO з best lap за замовчуванням (без ref lap params)")
    void getComparison_returnsDto_withDefaultBestLap() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        SessionSummaryDto summaryA = summaryWithBestLap(10, 3);
        SessionSummaryDto summaryB = summaryWithBestLap(10, 5);
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(summaryA);
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(summaryB);

        List<LapResponseDto> lapsA = List.of(
                LapResponseDto.builder().lapNumber(1).lapTimeMs(88_000).build(),
                LapResponseDto.builder().lapNumber(3).lapTimeMs(87_200).build());
        List<LapResponseDto> lapsB = List.of(
                LapResponseDto.builder().lapNumber(5).lapTimeMs(87_500).build());
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(lapsA);
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(lapsB);
        when(lapQueryService.getPace(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(Collections.emptyList());
        when(lapQueryService.getPace(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(Collections.emptyList());
        when(lapQueryService.getLapTrace(SESSION_PUBLIC_ID_STR, 3, (short) 0)).thenReturn(Collections.emptyList());
        when(lapQueryService.getLapTrace(SESSION_PUBLIC_ID_STR, 5, (short) 1)).thenReturn(Collections.emptyList());
        when(lapQueryService.getSpeedTrace(SESSION_PUBLIC_ID_STR, 3, (short) 0)).thenReturn(Collections.emptyList());
        when(lapQueryService.getSpeedTrace(SESSION_PUBLIC_ID_STR, 5, (short) 1)).thenReturn(Collections.emptyList());

        // Act
        ComparisonResponseDto result = service.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, null, null);

        // Assert
        assertThat(result.getSessionUid()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(result.getCarIndexA()).isEqualTo(0);
        assertThat(result.getCarIndexB()).isEqualTo(1);
        assertThat(result.getLapsA()).isEqualTo(lapsA);
        assertThat(result.getLapsB()).isEqualTo(lapsB);
        assertThat(result.getSummaryA()).isEqualTo(summaryA);
        assertThat(result.getSummaryB()).isEqualTo(summaryB);
        assertThat(result.getReferenceLapNumA()).isEqualTo(3);
        assertThat(result.getReferenceLapNumB()).isEqualTo(5);
        verify(lapQueryService).getLapTrace(SESSION_PUBLIC_ID_STR, 3, (short) 0);
        verify(lapQueryService).getLapTrace(SESSION_PUBLIC_ID_STR, 5, (short) 1);
    }

    @Test
    @DisplayName("getComparison використовує custom referenceLapNumA та referenceLapNumB")
    void getComparison_usesCustomReferenceLaps() {
        // Arrange
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session());
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(summaryWithBestLap(10, 3));
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(summaryWithBestLap(10, 5));
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(List.of());
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(List.of());
        when(lapQueryService.getPace(any(), any())).thenReturn(Collections.emptyList());
        when(lapQueryService.getLapTrace(SESSION_PUBLIC_ID_STR, 7, (short) 0)).thenReturn(Collections.emptyList());
        when(lapQueryService.getLapTrace(SESSION_PUBLIC_ID_STR, 9, (short) 1)).thenReturn(Collections.emptyList());
        when(lapQueryService.getSpeedTrace(SESSION_PUBLIC_ID_STR, 7, (short) 0)).thenReturn(Collections.emptyList());
        when(lapQueryService.getSpeedTrace(SESSION_PUBLIC_ID_STR, 9, (short) 1)).thenReturn(Collections.emptyList());

        // Act
        ComparisonResponseDto result = service.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, 7, 9);

        // Assert
        assertThat(result.getReferenceLapNumA()).isEqualTo(7);
        assertThat(result.getReferenceLapNumB()).isEqualTo(9);
        verify(lapQueryService).getLapTrace(SESSION_PUBLIC_ID_STR, 7, (short) 0);
        verify(lapQueryService).getSpeedTrace(SESSION_PUBLIC_ID_STR, 9, (short) 1);
    }

    @Test
    @DisplayName("getComparison кидає 404 коли немає даних для car A")
    void getComparison_throws404_whenNoDataForCarA() {
        // Arrange
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session());
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 0))
                .thenReturn(SessionSummaryMapper.emptySummaryDto());
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(summaryWithBestLap(10, 5));
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> service.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, null, null))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("No laps or summary for car index 0");
    }

    @Test
    @DisplayName("getComparison кидає 404 коли немає даних для car B")
    void getComparison_throws404_whenNoDataForCarB() {
        // Arrange: car A has summary, car B has empty summary and no laps
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session());
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 0)).thenReturn(summaryWithBestLap(10, 3));
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, (short) 1))
                .thenReturn(SessionSummaryMapper.emptySummaryDto());
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, (short) 1)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> service.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, null, null))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("No laps or summary for car index 1");
    }
}
