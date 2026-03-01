package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackCornerMapService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.TRACK_LAYOUT_TRACK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackController")
class TrackControllerTest {

    @Mock
    private TrackCornerMapService trackCornerMapService;
    @Mock
    private TrackLayoutService trackLayoutService;

    @InjectMocks
    private TrackController controller;

    @Test
    @DisplayName("getLayout повертає 200 з body коли layout є")
    void getLayout_returnsOk_whenPresent() {
        TrackLayoutResponseDto dto = TrackLayoutResponseDto.builder()
                .trackId((int) TRACK_LAYOUT_TRACK_ID)
                .points(java.util.List.of())
                .build();
        when(trackLayoutService.getLayout(TRACK_LAYOUT_TRACK_ID)).thenReturn(Optional.of(dto));

        ResponseEntity<TrackLayoutResponseDto> response = controller.getLayout(TRACK_LAYOUT_TRACK_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTrackId()).isEqualTo((int) TRACK_LAYOUT_TRACK_ID);
    }

    @Test
    @DisplayName("getLayout повертає 404 коли layout відсутній")
    void getLayout_returnsNotFound_whenAbsent() {
        when(trackLayoutService.getLayout((short) 99)).thenReturn(Optional.empty());

        ResponseEntity<TrackLayoutResponseDto> response = controller.getLayout((short) 99);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
