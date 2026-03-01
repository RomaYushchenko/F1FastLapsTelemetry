package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.LeaderboardQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardController")
class LeaderboardControllerTest {

    @Mock
    private LeaderboardQueryService leaderboardQueryService;

    @InjectMocks
    private LeaderboardController controller;

    @Test
    @DisplayName("getLeaderboard повертає 204 коли немає активної сесії")
    void getLeaderboard_returnsNoContent_whenEmpty() {
        when(leaderboardQueryService.getLeaderboardForActiveSession()).thenReturn(List.of());

        ResponseEntity<List<LeaderboardEntryDto>> response = controller.getLeaderboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("getLeaderboard повертає 200 з списком записів")
    void getLeaderboard_returnsOkWithBody_whenPresent() {
        List<LeaderboardEntryDto> entries = List.of(
                LeaderboardEntryDto.builder().position(1).carIndex(0).driverLabel("VER").compound("S").gap("LEAD").build()
        );
        when(leaderboardQueryService.getLeaderboardForActiveSession()).thenReturn(entries);

        ResponseEntity<List<LeaderboardEntryDto>> response = controller.getLeaderboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getDriverLabel()).isEqualTo("VER");
    }
}
