package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapCornerDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PitStopDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.StintDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.LapQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.PitStopQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.StintQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LapController")
class LapControllerTest {

    @Mock
    private LapQueryService lapQueryService;
    @Mock
    private PitStopQueryService pitStopQueryService;
    @Mock
    private StintQueryService stintQueryService;

    @InjectMocks
    private LapController controller;

    @Test
    @DisplayName("getLaps делегує сервісу та повертає 200")
    void getLaps_delegatesAndReturnsOk() {
        // Arrange
        List<LapResponseDto> list = List.of(LapResponseDto.builder().lapTimeMs(LAP_TIME_MS).build());
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<LapResponseDto>> response = controller.getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        verify(lapQueryService).getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }

    @Test
    @DisplayName("getPace делегує сервісу та повертає 200")
    void getPace_delegatesAndReturnsOk() {
        // Arrange
        List<PacePointDto> list = List.of(PacePointDto.builder().lapTimeMs(LAP_TIME_MS).build());
        when(lapQueryService.getPace(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<PacePointDto>> response = controller.getPace(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getBody()).hasSize(1);
        verify(lapQueryService).getPace(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }

    @Test
    @DisplayName("getTyreWear делегує сервісу та повертає 200")
    void getTyreWear_delegatesAndReturnsOk() {
        // Arrange
        List<TyreWearPointDto> list = List.of();
        when(lapQueryService.getTyreWear(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<TyreWearPointDto>> response = controller.getTyreWear(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getBody()).isEmpty();
        verify(lapQueryService).getTyreWear(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }

    @Test
    @DisplayName("getLapTrace делегує сервісу та повертає 200")
    void getLapTrace_delegatesAndReturnsOk() {
        // Arrange
        List<TracePointDto> list = List.of();
        when(lapQueryService.getLapTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<TracePointDto>> response = controller.getLapTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(response.getBody()).isEmpty();
        verify(lapQueryService).getLapTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);
    }

    @Test
    @DisplayName("getLapTrace кидає виняток коли lapNum null")
    void getLapTrace_throws_whenLapNumNull() {
        // Act & Assert
        assertThatThrownBy(() -> controller.getLapTrace(SESSION_PUBLIC_ID_STR, null, CAR_INDEX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lapNum is required");
    }

    @Test
    @DisplayName("getLapCorners делегує сервісу та повертає 200")
    void getLapCorners_delegatesAndReturnsOk() {
        // Arrange
        List<LapCornerDto> list = List.of(
                LapCornerDto.builder().cornerIndex(1).startDistanceM(100f).apexDistanceM(150f).endDistanceM(200f).build());
        when(lapQueryService.getCorners(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<LapCornerDto>> response = controller.getLapCorners(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCornerIndex()).isEqualTo(1);
        verify(lapQueryService).getCorners(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);
    }

    @Test
    @DisplayName("getLapSpeedTrace делегує сервісу та повертає 200")
    void getLapSpeedTrace_delegatesAndReturnsOk() {
        // Arrange
        List<SpeedTracePointDto> list = List.of(
                SpeedTracePointDto.builder().distanceM(100f).speedKph(250).build());
        when(lapQueryService.getSpeedTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<SpeedTracePointDto>> response = controller.getLapSpeedTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getDistanceM()).isEqualTo(100f);
        assertThat(response.getBody().get(0).getSpeedKph()).isEqualTo(250);
        verify(lapQueryService).getSpeedTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);
    }

    @Test
    @DisplayName("getLapSpeedTrace кидає виняток коли lapNum null")
    void getLapSpeedTrace_throws_whenLapNumNull() {
        // Act & Assert
        assertThatThrownBy(() -> controller.getLapSpeedTrace(SESSION_PUBLIC_ID_STR, null, CAR_INDEX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lapNum is required");
    }

    @Test
    @DisplayName("getSectors делегує getLaps сервісу")
    void getSectors_delegatesToGetLaps() {
        // Arrange
        List<LapResponseDto> list = List.of();
        when(lapQueryService.getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<LapResponseDto>> response = controller.getSectors(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getBody()).isEmpty();
        verify(lapQueryService).getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }

    @Test
    @DisplayName("getPitStops делегує сервісу та повертає 200")
    void getPitStops_delegatesAndReturnsOk() {
        // Arrange
        List<PitStopDto> list = List.of(
                PitStopDto.builder().lapNumber(2).compoundIn(18).compoundOut(16).build());
        when(pitStopQueryService.getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<PitStopDto>> response = controller.getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getLapNumber()).isEqualTo(2);
        verify(pitStopQueryService).getPitStops(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }

    @Test
    @DisplayName("getStints делегує сервісу та повертає 200")
    void getStints_delegatesAndReturnsOk() {
        // Arrange
        List<StintDto> list = List.of(
                StintDto.builder().stintIndex(1).compound(18).startLap(1).lapCount(5).build());
        when(stintQueryService.getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX)).thenReturn(list);

        // Act
        ResponseEntity<List<StintDto>> response = controller.getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStintIndex()).isEqualTo(1);
        verify(stintQueryService).getStints(SESSION_PUBLIC_ID_STR, CAR_INDEX);
    }
}
