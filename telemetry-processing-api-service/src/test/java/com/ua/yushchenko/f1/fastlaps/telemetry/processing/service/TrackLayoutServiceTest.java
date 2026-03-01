package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackLayoutService")
class TrackLayoutServiceTest {

    @Mock
    private TrackLayoutRepository trackLayoutRepository;

    private TrackLayoutService service;

    @BeforeEach
    void setUp() {
        service = new TrackLayoutService(trackLayoutRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("getLayout повертає DTO коли layout існує")
    void getLayout_returnsDto_whenPresent() {
        // Arrange
        TrackLayout entity = trackLayout();
        when(trackLayoutRepository.findById(TRACK_LAYOUT_TRACK_ID)).thenReturn(Optional.of(entity));

        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout(TRACK_LAYOUT_TRACK_ID);

        // Assert
        assertThat(result).isPresent();
        TrackLayoutResponseDto dto = result.get();
        assertThat(dto.getTrackId()).isEqualTo((int) TRACK_LAYOUT_TRACK_ID);
        assertThat(dto.getPoints()).hasSize(4);
        assertThat(dto.getPoints().get(0).getX()).isEqualTo(100.0);
        assertThat(dto.getPoints().get(0).getY()).isEqualTo(300.0);
        assertThat(dto.getBounds()).isNotNull();
        assertThat(dto.getBounds().getMinX()).isEqualTo(100.0);
        assertThat(dto.getBounds().getMaxY()).isEqualTo(500.0);
        verify(trackLayoutRepository).findById(TRACK_LAYOUT_TRACK_ID);
    }

    @Test
    @DisplayName("getLayout повертає empty коли trackId не знайдено")
    void getLayout_returnsEmpty_whenNotFound() {
        // Arrange
        when(trackLayoutRepository.findById((short) 99)).thenReturn(Optional.empty());

        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout((short) 99);

        // Assert
        assertThat(result).isEmpty();
        verify(trackLayoutRepository).findById((short) 99);
    }

    @Test
    @DisplayName("getLayout повертає empty коли trackId null")
    void getLayout_returnsEmpty_whenTrackIdNull() {
        // Act
        Optional<TrackLayoutResponseDto> result = service.getLayout(null);

        // Assert
        assertThat(result).isEmpty();
        // Repository is not called when trackId is null
    }
}
