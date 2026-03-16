package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.PacketHealthProperties;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.DiagnosticsMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics.PacketLossMetricsReader;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticsService")
class DiagnosticsServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private PacketLossMetricsReader packetLossMetricsReader;
    @Mock
    private DiagnosticsMapper diagnosticsMapper;
    @Mock
    private PacketHealthProperties packetHealthProperties;

    @InjectMocks
    private DiagnosticsService diagnosticsService;

    @Test
    @DisplayName("getSessionDiagnostics returns GOOD band when percent above good threshold")
    void getSessionDiagnosticsReturnsGoodBand() {
        Session session = TestData.session();
        when(sessionResolveService.getSessionByPublicIdOrUid(anyString())).thenReturn(session);
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.of(0.05d)); // 95%
        when(packetHealthProperties.getGoodMinPercent()).thenReturn(90);
        when(packetHealthProperties.getOkMinPercent()).thenReturn(70);

        when(diagnosticsMapper.toSessionDiagnosticsDto(
                TestData.SESSION_PUBLIC_ID_STR,
                0.05d,
                95,
                "GOOD"
        )).thenReturn(SessionDiagnosticsDto.builder()
                .sessionPublicId(TestData.SESSION_PUBLIC_ID_STR)
                .packetLossRatio(0.05d)
                .packetHealthPercent(95)
                .packetHealthBand("GOOD")
                .build());

        SessionDiagnosticsDto dto = diagnosticsService.getSessionDiagnostics(TestData.SESSION_PUBLIC_ID_STR);

        assertThat(dto.getPacketLossRatio()).isEqualTo(0.05d);
        assertThat(dto.getPacketHealthPercent()).isEqualTo(95);
        assertThat(dto.getPacketHealthBand()).isEqualTo("GOOD");
    }

    @Test
    @DisplayName("getSessionDiagnostics returns OK band when percent between thresholds")
    void getSessionDiagnosticsReturnsOkBand() {
        Session session = TestData.session();
        when(sessionResolveService.getSessionByPublicIdOrUid(anyString())).thenReturn(session);
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.of(0.25d)); // 75%
        when(packetHealthProperties.getGoodMinPercent()).thenReturn(90);
        when(packetHealthProperties.getOkMinPercent()).thenReturn(70);

        when(diagnosticsMapper.toSessionDiagnosticsDto(
                TestData.SESSION_PUBLIC_ID_STR,
                0.25d,
                75,
                "OK"
        )).thenReturn(SessionDiagnosticsDto.builder()
                .sessionPublicId(TestData.SESSION_PUBLIC_ID_STR)
                .packetLossRatio(0.25d)
                .packetHealthPercent(75)
                .packetHealthBand("OK")
                .build());

        SessionDiagnosticsDto dto = diagnosticsService.getSessionDiagnostics(TestData.SESSION_PUBLIC_ID_STR);

        assertThat(dto.getPacketHealthPercent()).isEqualTo(75);
        assertThat(dto.getPacketHealthBand()).isEqualTo("OK");
    }

    @Test
    @DisplayName("getSessionDiagnostics returns POOR band when percent below ok threshold")
    void getSessionDiagnosticsReturnsPoorBand() {
        Session session = TestData.session();
        when(sessionResolveService.getSessionByPublicIdOrUid(anyString())).thenReturn(session);
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.of(0.6d)); // 40%
        when(packetHealthProperties.getGoodMinPercent()).thenReturn(90);
        when(packetHealthProperties.getOkMinPercent()).thenReturn(70);

        when(diagnosticsMapper.toSessionDiagnosticsDto(
                TestData.SESSION_PUBLIC_ID_STR,
                0.6d,
                40,
                "POOR"
        )).thenReturn(SessionDiagnosticsDto.builder()
                .sessionPublicId(TestData.SESSION_PUBLIC_ID_STR)
                .packetLossRatio(0.6d)
                .packetHealthPercent(40)
                .packetHealthBand("POOR")
                .build());

        SessionDiagnosticsDto dto = diagnosticsService.getSessionDiagnostics(TestData.SESSION_PUBLIC_ID_STR);

        assertThat(dto.getPacketHealthPercent()).isEqualTo(40);
        assertThat(dto.getPacketHealthBand()).isEqualTo("POOR");
    }

    @Test
    @DisplayName("getSessionDiagnostics returns UNKNOWN band when metric missing")
    void getSessionDiagnosticsReturnsUnknownWhenMetricMissing() {
        Session session = TestData.session();
        when(sessionResolveService.getSessionByPublicIdOrUid(anyString())).thenReturn(session);
        when(packetLossMetricsReader.getPacketLossRatioBySessionUid(TestData.SESSION_UID))
                .thenReturn(Optional.empty());

        when(diagnosticsMapper.toSessionDiagnosticsDto(
                TestData.SESSION_PUBLIC_ID_STR,
                null,
                null,
                "UNKNOWN"
        )).thenReturn(SessionDiagnosticsDto.builder()
                .sessionPublicId(TestData.SESSION_PUBLIC_ID_STR)
                .packetLossRatio(null)
                .packetHealthPercent(null)
                .packetHealthBand("UNKNOWN")
                .build());

        SessionDiagnosticsDto dto = diagnosticsService.getSessionDiagnostics(TestData.SESSION_PUBLIC_ID_STR);

        assertThat(dto.getPacketLossRatio()).isNull();
        assertThat(dto.getPacketHealthPercent()).isNull();
        assertThat(dto.getPacketHealthBand()).isEqualTo("UNKNOWN");
    }
}

