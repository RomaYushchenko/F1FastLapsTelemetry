package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.GlobalDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.PacketHealthProperties;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.PacketLossMetricsReader;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalDiagnosticsService")
class GlobalDiagnosticsServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private PacketLossMetricsReader packetLossMetricsReader;
    @Mock
    private PacketHealthProperties packetHealthProperties;

    @InjectMocks
    private GlobalDiagnosticsService globalDiagnosticsService;

    @Test
    @DisplayName("getGlobalDiagnostics returns UNKNOWN when no metrics available")
    void getGlobalDiagnosticsReturnsUnknownWhenNoMetrics() {
        Session session = TestData.session();
        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.empty());

        GlobalDiagnosticsDto dto = globalDiagnosticsService.getGlobalDiagnostics();

        assertThat(dto.getPacketLossRatio()).isNull();
        assertThat(dto.getPacketHealthPercent()).isNull();
        assertThat(dto.getPacketHealthBand()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("getGlobalDiagnostics aggregates metrics and maps to band")
    void getGlobalDiagnosticsAggregatesMetrics() {
        Session session = TestData.session();
        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.of(0.1d)); // 90%
        when(packetHealthProperties.getGoodMinPercent()).thenReturn(90);
        when(packetHealthProperties.getOkMinPercent()).thenReturn(70);

        GlobalDiagnosticsDto dto = globalDiagnosticsService.getGlobalDiagnostics();

        assertThat(dto.getPacketLossRatio()).isEqualTo(0.1d);
        assertThat(dto.getPacketHealthPercent()).isEqualTo(90);
        assertThat(dto.getPacketHealthBand()).isEqualTo("GOOD");
    }
}

