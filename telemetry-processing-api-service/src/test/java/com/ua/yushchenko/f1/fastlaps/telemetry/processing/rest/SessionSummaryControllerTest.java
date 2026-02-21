package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionSummaryQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionSummaryController")
class SessionSummaryControllerTest {

    @Mock
    private SessionSummaryQueryService sessionSummaryQueryService;

    @InjectMocks
    private SessionSummaryController controller;

    @Test
    @DisplayName("getSummary делегує сервісу та повертає 200 з DTO")
    void getSummary_delegatesAndReturnsOk() {
        // Arrange
        SessionSummaryDto dto = SessionSummaryDto.builder().totalLaps(10).build();
        when(sessionSummaryQueryService.getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(dto);

        // Act
        ResponseEntity<SessionSummaryDto> response = controller.getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(dto);
        assertThat(response.getBody().getTotalLaps()).isEqualTo(10);
        verify(sessionSummaryQueryService).getSummary(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }
}
