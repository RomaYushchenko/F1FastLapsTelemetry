package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionEventsQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_PUBLIC_ID_STR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionEventsController")
class SessionEventsControllerTest {

    @Mock
    private SessionEventsQueryService sessionEventsQueryService;

    @InjectMocks
    private SessionEventsController controller;

    @Test
    @DisplayName("getEvents повертає 200 з списком подій")
    void getEvents_returnsOkWithBody() {
        List<SessionEventDto> events = List.of(
                SessionEventDto.builder().lap(24).eventCode("FTLP").carIndex(0).build()
        );
        when(sessionEventsQueryService.getEvents(SESSION_PUBLIC_ID_STR, null, null, null)).thenReturn(events);

        ResponseEntity<List<SessionEventDto>> response = controller.getEvents(SESSION_PUBLIC_ID_STR, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getEventCode()).isEqualTo("FTLP");
    }
}
