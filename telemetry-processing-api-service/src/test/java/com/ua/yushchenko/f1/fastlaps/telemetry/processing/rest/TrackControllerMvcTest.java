package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SectorBoundaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutBoundsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackLayoutStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.TRACK_LAYOUT_TRACK_ID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TrackController.class)
@DisplayName("TrackController MVC")
class TrackControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrackLayoutService trackLayoutService;

    @Test
    @DisplayName("GET /api/tracks/{trackId}/layout повертає z, minElev та sectorBoundaries")
    void getLayout_returns3dPoints_boundsAndSectors() throws Exception {
        // Arrange
        TrackLayoutPointDto p1 = TrackLayoutPointDto.builder()
                .x(100.0)
                .y(3.0)
                .z(300.0)
                .build();
        TrackLayoutPointDto p2 = TrackLayoutPointDto.builder()
                .x(200.0)
                .y(5.0)
                .z(400.0)
                .build();
        TrackLayoutBoundsDto bounds = TrackLayoutBoundsDto.builder()
                .minX(100.0)
                .maxX(200.0)
                .minZ(300.0)
                .maxZ(400.0)
                .minElev(3.0)
                .maxElev(5.0)
                .build();
        List<SectorBoundaryDto> sectors = List.of(
                new SectorBoundaryDto(1, 100.0, 3.0, 300.0),
                new SectorBoundaryDto(2, 150.0, 4.0, 350.0),
                new SectorBoundaryDto(3, 200.0, 5.0, 400.0)
        );
        TrackLayoutResponseDto dto = TrackLayoutResponseDto.builder()
                .trackId((int) TRACK_LAYOUT_TRACK_ID)
                .points(List.of(p1, p2))
                .bounds(bounds)
                .source("RECORDED")
                .sectorBoundaries(sectors)
                .build();
        when(trackLayoutService.getLayout(TRACK_LAYOUT_TRACK_ID)).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/tracks/{trackId}/layout", TRACK_LAYOUT_TRACK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value((int) TRACK_LAYOUT_TRACK_ID))
                .andExpect(jsonPath("$.points[0].z").value(300.0))
                .andExpect(jsonPath("$.bounds.minElev").value(3.0))
                .andExpect(jsonPath("$.sectorBoundaries.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/tracks/{trackId}/layout/status повертає статус та pointsCollected")
    void getLayoutStatus_returnsStatusAndPoints() throws Exception {
        short trackId = TRACK_LAYOUT_TRACK_ID;
        TrackLayoutStatusDto statusDto = new TrackLayoutStatusDto(
                trackId,
                "READY",
                512,
                "RECORDED"
        );
        when(trackLayoutService.getLayoutStatus(trackId)).thenReturn(statusDto);

        mockMvc.perform(get("/api/tracks/{trackId}/layout/status", trackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value((int) trackId))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.pointsCollected").value(512))
                .andExpect(jsonPath("$.source").value("RECORDED"));
    }
}

