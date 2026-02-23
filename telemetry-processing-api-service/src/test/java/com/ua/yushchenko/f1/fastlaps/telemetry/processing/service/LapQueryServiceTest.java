package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapCornerDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner.SteerBasedCornerSegmenter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarStatusRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LapQueryService")
class LapQueryServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private LapRepository lapRepository;
    @Mock
    private CarTelemetryRawRepository carTelemetryRawRepository;
    @Mock
    private CarStatusRawRepository carStatusRawRepository;
    @Mock
    private TyreWearPerLapRepository tyreWearPerLapRepository;
    @Spy
    private LapMapper lapMapper = new LapMapper();
    @Spy
    private SteerBasedCornerSegmenter cornerSegmenter = new SteerBasedCornerSegmenter();

    @InjectMocks
    private LapQueryService service;

    @Test
    @DisplayName("getLaps резолвить сесію та повертає змаплені кола")
    void getLaps_resolvesSessionAndReturnsMappedLaps() {
        // Arrange
        Session session = session();
        Lap lap = lap();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX))
                .thenReturn(List.of(lap));

        // Act
        List<LapResponseDto> result = service.getLaps(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLapTimeMs()).isEqualTo(LAP_TIME_MS);
        verify(sessionResolveService).getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getPace фільтрує null pace points")
    void getPace_filtersNullPacePoints() {
        // Arrange
        Session session = session();
        Lap lapValid = lap();
        Lap lapZero = lapZeroTime();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX))
                .thenReturn(List.of(lapZero, lapValid));

        // Act
        List<PacePointDto> result = service.getPace(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLapTimeMs()).isEqualTo(LAP_TIME_MS);
    }

    @Test
    @DisplayName("getTyreWear повертає змаплені DTO")
    void getTyreWear_returnsMappedDtos() {
        // Arrange
        Session session = session();
        TyreWearPerLap row = tyreWearPerLap();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(SESSION_UID, CAR_INDEX))
                .thenReturn(List.of(row));

        // Act
        List<TyreWearPointDto> result = service.getTyreWear(SESSION_PUBLIC_ID_STR, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWearFL()).isEqualTo(WEAR_FL);
    }

    @Test
    @DisplayName("getLapTrace повертає змаплені trace points")
    void getLapTrace_returnsMappedTracePoints() {
        // Arrange
        Session session = session();
        CarTelemetryRaw raw = carTelemetryRaw();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(carTelemetryRawRepository.findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                SESSION_UID, CAR_INDEX, (short) 1)).thenReturn(List.of(raw));

        // Act
        List<TracePointDto> result = service.getLapTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDistance()).isEqualTo(LAP_DISTANCE_M);
    }

    @Test
    @DisplayName("getSpeedTrace повертає змаплені speed trace points")
    void getSpeedTrace_returnsMappedSpeedTracePoints() {
        // Arrange
        Session session = session();
        CarTelemetryRaw raw = carTelemetryRaw();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(carTelemetryRawRepository.findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                SESSION_UID, CAR_INDEX, (short) 1)).thenReturn(List.of(raw));

        // Act
        List<SpeedTracePointDto> result = service.getSpeedTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDistanceM()).isEqualTo(LAP_DISTANCE_M);
        assertThat(result.get(0).getSpeedKph()).isEqualTo(SPEED_KPH);
        verify(carTelemetryRawRepository).findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                SESSION_UID, CAR_INDEX, (short) 1);
    }

    @Test
    @DisplayName("getCorners повертає порожній список коли немає телеметрії з steer")
    void getCorners_returnsEmpty_whenNoTelemetry() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(carTelemetryRawRepository.findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                SESSION_UID, CAR_INDEX, (short) 1)).thenReturn(List.of());

        // Act
        List<LapCornerDto> result = service.getCorners(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSpeedTrace повертає порожній список коли немає телеметрії")
    void getSpeedTrace_returnsEmpty_whenNoTelemetry() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(carTelemetryRawRepository.findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                SESSION_UID, CAR_INDEX, (short) 1)).thenReturn(List.of());

        // Act
        List<SpeedTracePointDto> result = service.getSpeedTrace(SESSION_PUBLIC_ID_STR, 1, CAR_INDEX);

        // Assert
        assertThat(result).isEmpty();
    }
}
