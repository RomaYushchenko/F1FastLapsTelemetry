package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.GlobalDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.DiagnosticsService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.GlobalDiagnosticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiagnosticsController.class)
@DisplayName("DiagnosticsController")
class DiagnosticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiagnosticsService diagnosticsService;
    @MockBean
    private GlobalDiagnosticsService globalDiagnosticsService;

    @Test
    @DisplayName("GET /api/sessions/{publicId}/diagnostics returns diagnostics DTO")
    void getSessionDiagnosticsReturnsDto() throws Exception {
        SessionDiagnosticsDto dto = SessionDiagnosticsDto.builder()
                .sessionPublicId("session-id")
                .packetLossRatio(0.1d)
                .packetHealthPercent(90)
                .packetHealthBand("GOOD")
                .build();

        Mockito.when(diagnosticsService.getSessionDiagnostics(eq("session-id"))).thenReturn(dto);

        mockMvc.perform(get("/api/sessions/{publicId}/diagnostics", "session-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionPublicId").value("session-id"))
                .andExpect(jsonPath("$.packetLossRatio").value(0.1d))
                .andExpect(jsonPath("$.packetHealthPercent").value(90))
                .andExpect(jsonPath("$.packetHealthBand").value("GOOD"));
    }

    @Test
    @DisplayName("GET /api/diagnostics returns global diagnostics DTO")
    void getGlobalDiagnosticsReturnsDto() throws Exception {
        GlobalDiagnosticsDto dto = GlobalDiagnosticsDto.builder()
                .statusMessage("OK")
                .packetLossRatio(0.2d)
                .packetHealthPercent(80)
                .packetHealthBand("OK")
                .build();

        Mockito.when(globalDiagnosticsService.getGlobalDiagnostics()).thenReturn(dto);

        mockMvc.perform(get("/api/diagnostics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusMessage").value("OK"))
                .andExpect(jsonPath("$.packetLossRatio").value(0.2d))
                .andExpect(jsonPath("$.packetHealthPercent").value(80))
                .andExpect(jsonPath("$.packetHealthBand").value("OK"));
    }
}

