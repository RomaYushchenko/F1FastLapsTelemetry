package com.ua.yushchenko.f1.fastlaps.telemetry.processing.diagnostics;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDiagnosticsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DiagnosticsMapper")
class DiagnosticsMapperTest {

    private final DiagnosticsMapper mapper = new DiagnosticsMapper();

    @Test
    @DisplayName("toSessionDiagnosticsDto maps all fields correctly")
    void toSessionDiagnosticsDtoMapsAllFieldsCorrectly() {
        SessionDiagnosticsDto dto = mapper.toSessionDiagnosticsDto(
                "session-id",
                0.25d,
                75,
                "OK"
        );

        assertThat(dto.getSessionPublicId()).isEqualTo("session-id");
        assertThat(dto.getPacketLossRatio()).isEqualTo(0.25d);
        assertThat(dto.getPacketHealthPercent()).isEqualTo(75);
        assertThat(dto.getPacketHealthBand()).isEqualTo("OK");
    }

    @Test
    @DisplayName("toSessionDiagnosticsDto allows null values")
    void toSessionDiagnosticsDtoAllowsNullValues() {
        SessionDiagnosticsDto dto = mapper.toSessionDiagnosticsDto(
                "session-id",
                null,
                null,
                "UNKNOWN"
        );

        assertThat(dto.getPacketLossRatio()).isNull();
        assertThat(dto.getPacketHealthPercent()).isNull();
        assertThat(dto.getPacketHealthBand()).isEqualTo("UNKNOWN");
    }
}

