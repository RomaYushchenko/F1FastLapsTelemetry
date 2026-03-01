package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_PUBLIC_ID_STR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionExportService")
class SessionExportServiceTest {

    @Mock
    private SessionQueryService sessionQueryService;
    @Mock
    private LapQueryService lapQueryService;
    @Mock
    private SessionSummaryQueryService sessionSummaryQueryService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SessionExportService service;

    private SessionDto sessionDto;
    private SessionSummaryDto summaryDto;
    private List<LapResponseDto> laps;

    @BeforeEach
    void setUp() {
        sessionDto = SessionDto.builder().id(SESSION_PUBLIC_ID_STR).playerCarIndex(0).build();
        summaryDto = SessionSummaryDto.builder().totalLaps(5).bestLapTimeMs(87_321).build();
        laps = List.of(
                LapResponseDto.builder().lapNumber(1).lapTimeMs(87_500).positionAtLapStart(3).build(),
                LapResponseDto.builder().lapNumber(2).lapTimeMs(88_100).positionAtLapStart(3).build()
        );
    }

    @Test
    @DisplayName("buildExport json викликає сервіси і повертає JSON з summary та laps")
    void buildExportJson_callsServicesAndReturnsJson() throws IOException {
        when(sessionQueryService.getSession(SESSION_PUBLIC_ID_STR)).thenReturn(sessionDto);
        when(sessionSummaryQueryService.getSummary(eq(SESSION_PUBLIC_ID_STR), eq((short) 0))).thenReturn(summaryDto);
        when(lapQueryService.getLaps(eq(SESSION_PUBLIC_ID_STR), eq((short) 0))).thenReturn(laps);

        byte[] result = service.buildExport(SESSION_PUBLIC_ID_STR, SessionExportService.FORMAT_JSON);

        assertThat(result).isNotEmpty();
        String json = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json).contains("\"summary\"");
        assertThat(json).contains("\"laps\"");
        assertThat(json).contains("\"session\"");
        verify(sessionQueryService).getSession(SESSION_PUBLIC_ID_STR);
        verify(sessionSummaryQueryService).getSummary(SESSION_PUBLIC_ID_STR, (short) 0);
        verify(lapQueryService).getLaps(SESSION_PUBLIC_ID_STR, (short) 0);
    }

    @Test
    @DisplayName("buildExport csv повертає CSV з заголовком і рядками по колах")
    void buildExportCsv_returnsCsvWithHeaderAndRows() throws IOException {
        when(sessionQueryService.getSession(SESSION_PUBLIC_ID_STR)).thenReturn(sessionDto);
        when(sessionSummaryQueryService.getSummary(eq(SESSION_PUBLIC_ID_STR), eq((short) 0))).thenReturn(summaryDto);
        when(lapQueryService.getLaps(eq(SESSION_PUBLIC_ID_STR), eq((short) 0))).thenReturn(laps);

        byte[] result = service.buildExport(SESSION_PUBLIC_ID_STR, SessionExportService.FORMAT_CSV);

        assertThat(result).isNotEmpty();
        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("lapNumber,lapTimeMs,sector1Ms,sector2Ms,sector3Ms,isInvalid,positionAtLapStart");
        assertThat(csv).contains("1,87500,,,,");
        assertThat(csv).contains("2,88100,,,,");
    }
}
