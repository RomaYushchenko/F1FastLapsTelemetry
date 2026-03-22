package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ComparisonResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionRaceOverviewDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.ComparisonQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.RaceOverviewQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionExportService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListResult;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController")
class SessionControllerTest {

    @Mock
    private SessionQueryService sessionQueryService;
    @Mock
    private SessionUpdateService sessionUpdateService;
    @Mock
    private ComparisonQueryService comparisonQueryService;
    @Mock
    private SessionExportService sessionExportService;
    @Mock
    private RaceOverviewQueryService raceOverviewQueryService;

    @InjectMocks
    private SessionController controller;

    @Test
    @DisplayName("listSessions делегує виклик сервісу і повертає X-Total-Count")
    void listSessions_delegatesToService() {
        // Arrange
        SessionDto dto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).build();
        when(sessionQueryService.listSessions(any())).thenReturn(new SessionListResult(List.of(dto), 142L));

        // Act
        ResponseEntity<List<SessionDto>> response = controller.listSessions(0, 50, null, null, null, null, null, null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(response.getHeaders().getFirst(SessionController.HEADER_TOTAL_COUNT)).isEqualTo("142");
        ArgumentCaptor<com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListFilter> filterCaptor =
                ArgumentCaptor.forClass(com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListFilter.class);
        verify(sessionQueryService).listSessions(filterCaptor.capture());
        assertThat(filterCaptor.getValue().getOffset()).isEqualTo(0);
        assertThat(filterCaptor.getValue().getLimit()).isEqualTo(50);
    }

    @Test
    @DisplayName("getSession повертає 200 OK з DTO")
    void getSession_returnsOkWithDto() {
        // Arrange
        SessionDto dto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).build();
        when(sessionQueryService.getSession(SESSION_PUBLIC_ID_STR)).thenReturn(dto);

        // Act
        ResponseEntity<SessionDto> response = controller.getSession(SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        verify(sessionQueryService).getSession(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getActiveSession повертає 204 No Content коли немає активної")
    void getActiveSession_returnsNoContent_whenEmpty() {
        // Arrange
        when(sessionQueryService.getActiveSession()).thenReturn(Optional.empty());

        // Act
        ResponseEntity<SessionDto> response = controller.getActiveSession();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("getActiveSession повертає 200 OK з DTO коли активна є")
    void getActiveSession_returnsOkWithDto_whenPresent() {
        // Arrange
        SessionDto dto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).build();
        when(sessionQueryService.getActiveSession()).thenReturn(Optional.of(dto));

        // Act
        ResponseEntity<SessionDto> response = controller.getActiveSession();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("updateSessionDisplayName повертає 200 OK з оновленим DTO")
    void updateSessionDisplayName_returnsOkWithUpdatedDto() {
        // Arrange
        UpdateSessionDisplayNameRequest request = new UpdateSessionDisplayNameRequest("Monaco Race");
        SessionDto dto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).sessionDisplayName("Monaco Race").build();
        when(sessionUpdateService.updateDisplayName(SESSION_PUBLIC_ID_STR, "Monaco Race")).thenReturn(dto);

        // Act
        ResponseEntity<SessionDto> response = controller.updateSessionDisplayName(SESSION_PUBLIC_ID_STR, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSessionDisplayName()).isEqualTo("Monaco Race");
        verify(sessionUpdateService).updateDisplayName(SESSION_PUBLIC_ID_STR, "Monaco Race");
    }

    @Test
    @DisplayName("getComparison делегує сервісу та повертає 200 з ComparisonResponseDto")
    void getComparison_delegatesAndReturnsOk() {
        // Arrange
        ComparisonResponseDto dto = ComparisonResponseDto.builder()
                .sessionUid(SESSION_PUBLIC_ID_STR)
                .carIndexA(0)
                .carIndexB(1)
                .referenceLapNumA(3)
                .referenceLapNumB(5)
                .build();
        when(comparisonQueryService.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, null, null)).thenReturn(dto);

        // Act
        ResponseEntity<ComparisonResponseDto> response = controller.getComparison(
                SESSION_PUBLIC_ID_STR, 0, 1, null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
        assertThat(response.getBody().getSessionUid()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(response.getBody().getCarIndexA()).isEqualTo(0);
        assertThat(response.getBody().getCarIndexB()).isEqualTo(1);
        verify(comparisonQueryService).getComparison(SESSION_PUBLIC_ID_STR, 0, 1, null, null);
    }

    @Test
    @DisplayName("getComparison передає referenceLapNumA та referenceLapNumB сервісу")
    void getComparison_passesReferenceLapParams() {
        // Arrange
        ComparisonResponseDto dto = ComparisonResponseDto.builder()
                .sessionUid(SESSION_PUBLIC_ID_STR)
                .carIndexA(0)
                .carIndexB(1)
                .referenceLapNumA(7)
                .referenceLapNumB(9)
                .build();
        when(comparisonQueryService.getComparison(SESSION_PUBLIC_ID_STR, 0, 1, 7, 9)).thenReturn(dto);

        // Act
        ResponseEntity<ComparisonResponseDto> response = controller.getComparison(
                SESSION_PUBLIC_ID_STR, 0, 1, 7, 9);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(comparisonQueryService).getComparison(SESSION_PUBLIC_ID_STR, 0, 1, 7, 9);
    }

    @Test
    @DisplayName("exportSession повертає 200 з тілом та Content-Disposition attachment")
    void exportSession_returnsOkWithBodyAndAttachmentHeader() throws IOException {
        // Arrange
        byte[] jsonBody = "{\"session\":{},\"summary\":{},\"laps\":[]}".getBytes();
        when(sessionExportService.buildExport(eq(SESSION_PUBLIC_ID_STR), eq("json"))).thenReturn(jsonBody);

        // Act
        ResponseEntity<byte[]> response = controller.exportSession(SESSION_PUBLIC_ID_STR, "json");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(jsonBody);
        assertThat(response.getHeaders().getContentDisposition()).isNotNull();
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("session-" + SESSION_PUBLIC_ID_STR + "-export.json");
        verify(sessionExportService).buildExport(SESSION_PUBLIC_ID_STR, "json");
    }

    @Test
    @DisplayName("exportSession кидає IllegalArgumentException для невалідного format")
    void exportSession_throwsWhenInvalidFormat() {
        assertThatThrownBy(() -> controller.exportSession(SESSION_PUBLIC_ID_STR, "xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format must be csv or json");
    }

    @Test
    @DisplayName("getSessionLeaderboard повертає 204 коли список порожній")
    void getSessionLeaderboard_returnsNoContent_whenEmpty() {
        when(raceOverviewQueryService.getLeaderboardForSession(SESSION_PUBLIC_ID_STR)).thenReturn(List.of());

        ResponseEntity<List<LeaderboardEntryDto>> response =
                controller.getSessionLeaderboard(SESSION_PUBLIC_ID_STR);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("getSessionRaceOverview повертає 204 коли entries порожні")
    void getSessionRaceOverview_returnsNoContent_whenEmptyEntries() {
        when(raceOverviewQueryService.getRaceOverview(SESSION_PUBLIC_ID_STR))
                .thenReturn(SessionRaceOverviewDto.builder().entries(List.of()).build());

        ResponseEntity<SessionRaceOverviewDto> response =
                controller.getSessionRaceOverview(SESSION_PUBLIC_ID_STR);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
