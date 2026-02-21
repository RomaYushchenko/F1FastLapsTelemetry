package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionSummaryQueryService")
class SessionSummaryQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private SessionSummaryRepository summaryRepository;
    @Mock
    private SessionFinishingPositionRepository finishingPositionRepository;
    @Spy
    private SessionSummaryMapper sessionSummaryMapper = new SessionSummaryMapper();

    @InjectMocks
    private SessionSummaryQueryService service;

    @Test
    @DisplayName("getSummary повертає DTO коли summary знайдено")
    void getSummary_returnsDto_whenSummaryFound() {
        // Arrange
        Session session = session();
        SessionSummary summary = sessionSummary();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(summaryRepository.findBySessionUidAndCarIndex(SESSION_UID, CAR_INDEX))
                .thenReturn(Optional.of(summary));
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(Collections.emptyList());

        // Act
        SessionSummaryDto dto = service.getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getTotalLaps()).isEqualTo(10);
        assertThat(dto.getBestLapTimeMs()).isEqualTo(87_200);
        verify(sessionResolveService).getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getSummary повертає порожній summary коли не знайдено")
    void getSummary_returnsEmptySummary_whenNotFound() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(summaryRepository.findBySessionUidAndCarIndex(SESSION_UID, CAR_INDEX))
                .thenReturn(Optional.empty());
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(Collections.emptyList());

        // Act
        SessionSummaryDto dto = service.getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(dto.getTotalLaps()).isEqualTo(0);
        assertThat(dto.getBestLapTimeMs()).isNull();
    }

    @Test
    @DisplayName("getSummary збагачує DTO лідером коли є finishing position P1")
    void getSummary_enrichesWithLeader_whenFinishingPositionP1Exists() {
        // Arrange
        Session session = session();
        session.setPlayerCarIndex(CAR_INDEX);
        SessionSummary summary = sessionSummary();
        SessionFinishingPosition leader = SessionFinishingPosition.builder()
                .sessionUid(SESSION_UID)
                .carIndex(CAR_INDEX)
                .finishingPosition(1)
                .build();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(summaryRepository.findBySessionUidAndCarIndex(SESSION_UID, CAR_INDEX))
                .thenReturn(Optional.of(summary));
        when(finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(SESSION_UID))
                .thenReturn(List.of(leader));

        // Act
        SessionSummaryDto dto = service.getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(dto.getLeaderPosition()).isEqualTo(1);
        assertThat(dto.getLeaderCarIndex()).isEqualTo(CAR_INDEX.intValue());
        assertThat(dto.getLeaderIsPlayer()).isTrue();
    }
}
