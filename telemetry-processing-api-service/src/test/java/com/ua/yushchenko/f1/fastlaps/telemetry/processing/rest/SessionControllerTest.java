package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest.UpdateSessionDisplayNameRequest;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController")
class SessionControllerTest {

    @Mock
    private SessionQueryService sessionQueryService;
    @Mock
    private SessionUpdateService sessionUpdateService;

    @InjectMocks
    private SessionController controller;

    @Test
    @DisplayName("listSessions делегує виклик сервісу")
    void listSessions_delegatesToService() {
        // Arrange
        SessionDto dto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).build();
        when(sessionQueryService.listSessions(0, 50)).thenReturn(List.of(dto));

        // Act
        List<SessionDto> result = controller.listSessions(0, 50);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        verify(sessionQueryService).listSessions(0, 50);
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
}
